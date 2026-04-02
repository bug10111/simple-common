package com.simple.oauth.service.sysUser;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.service.login.LoginService;
import com.simple.common.core.common.service.lock.LockService;
import com.simple.common.core.function.ReturnValueFunction;
import com.simple.common.core.utils.CryptoUtil;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.BeanUtils;
import com.simple.common.core.utils.IdUtils;
import com.simple.common.sms.common.service.SmsService;
import com.simple.oauth.common.dto.sysRole.FindOneSysRoleRequest;
import com.simple.oauth.common.dto.sysUser.*;
import com.simple.oauth.common.dto.wxLogin.WechatInfoDTO;
import com.simple.oauth.common.entity.sysRole.SysRole;
import com.simple.oauth.common.entity.sysUser.SysUser;
import com.simple.oauth.common.entity.sysUserLoginKey.SysUserLoginKey;
import com.simple.oauth.common.entity.sysUserRole.SysUserRole;
import com.simple.oauth.common.manager.username.SysUserNameCacheManager;
import com.simple.oauth.common.properties.OauthProperties;
import com.simple.oauth.common.service.sysUser.SysUserService;
import com.simple.oauth.common.view.sysRole.SysRoleView;
import com.simple.oauth.common.view.sysUser.SysUserView;
import com.simple.oauth.common.view.sysUserLoginKey.SysUserLoginKeyView;
import com.simple.oauth.common.view.sysUserRole.SysUserRoleView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 用户(sys_user)默认接口实现
 *
 * @author 兄台丶请冷静
 */
@Service
@Transactional
class DefaultSysUserService implements SysUserService {

    @Autowired
    private SysUserView sysUserView;

    @Autowired
    private SysRoleView sysRoleView;

    @Autowired
    private SysUserRoleView sysUserRoleView;

    @Autowired
    private LoginService loginService;

    @Autowired
    private LockService lockService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private SysUserNameCacheManager sysUserNameCacheManager;

    @Autowired
    private OauthProperties oauthProperties;

    @Autowired
    private SysUserLoginKeyView sysUserLoginKeyView;

    @Override
    public IPage<SysUserPageResponse> findAll(FindAllSysUserRequest findAllRequest) {
        var pageInfo = sysUserView.findAll(findAllRequest);
        return pageInfo.convert(entity -> BeanUtils.copyProperties(entity, SysUserPageResponse.class));
    }

    @Override
    public SysUserInfoResponse findById(String id) {
        var sysUser = sysUserView.findById(id);
        AssertUtils.notEmpty(sysUser, "用户不存在");
        return BeanUtils.copyProperties(sysUser, SysUserInfoResponse.class);
    }

    @Override
    public String findByName(String name) {
        SysUser one = sysUserView.findOne(new FindOneSysUserRequest().setUsername(name));
        AssertUtils.notEmpty(one, "用户不存在");
        return one.getId();
    }

    @Override
    public List<SysUserByRoleKeyResponse> findOneByRoleKey(String roleKey) {
        return sysUserView.findOneByRoleKey(roleKey).stream().filter(Objects::nonNull).map(sysUser -> BeanUtils.copyProperties(sysUser, SysUserByRoleKeyResponse.class)).toList();
    }

    @Override
    public List<SysUserByRoleKeyResponse> findOneByRoleId(String roleId) {
        return sysUserView.findOneByRoleId(roleId).stream().filter(Objects::nonNull).map(sysUser -> BeanUtils.copyProperties(sysUser, SysUserByRoleKeyResponse.class)).toList();
    }

    @Override
    @Transactional
    public String save(CreateSysUserRequest createRequest) {
        SysUser one1 = sysUserView.findOne(new FindOneSysUserRequest().setUsername(createRequest.getUsername()));
        if (one1 != null) {
            return one1.getId();
        }

        //        sysUserNameCacheManager.put(userId, createRequest.getNickname());

        if (one1 == null) {
            ReturnValueFunction function = () -> {
                SysUser sysUser = sysUserView.findOne(new FindOneSysUserRequest().setUsername(createRequest.getUsername()));
                AssertUtils.isTrue(ObjUtil.isNull(sysUser), "用户已存在");

                SysUser sysUser1 = sysUserView.findOne(new FindOneSysUserRequest().setPhone(createRequest.getPhone()));
                AssertUtils.isTrue(ObjUtil.isNull(sysUser1), "该手机号已被绑定");

                var entity = BeanUtils.copyProperties(createRequest, SysUser.class);
                entity.setPassword(CryptoUtil.hashPassword(createRequest.getPasswordNew()));
                sysUserView.saveOrUpdate(entity);
                return entity.getId();
            };

            String userId = lockService.lockHaveValue(createRequest.getUsername(), function).toString();

            //绑定角色
            if (createRequest.getRoleIds() != null && ObjUtil.isNotEmpty(createRequest.getRoleIds())) {
                bindingRole(userId, createRequest.getRoleIds());
            }

            return userId;
        } else {
            return one1.getId();
        }

    }

    @Override
    @Transactional
    public SysUser save(WechatInfoDTO wechatInfoDTO, ClientDetails clientDetails) {
        SysUser sysUser = new SysUser();
        sysUser.setUsername("txc" + IdUtils.randomNumbers());
        sysUser.setIsEnabled(1);
        sysUser.setIsAccountNonLocked(1);
        sysUser.setIsAccountNonExpired(1);
        sysUser.setIsCredentialsNonExpired(1);
        sysUserView.saveOrUpdate(sysUser);

        SysRole one = sysRoleView.findOne(new FindOneSysRoleRequest().setRoleKey(oauthProperties.getTourists()), false);

        SysUserRole sysUserRole = new SysUserRole();
        sysUserRole.setUserId(sysUser.getId());
        sysUserRole.setRoleId(one.getId());
        sysUserRoleView.saveOrUpdate(sysUserRole);

        sysUserLoginKeyView.save(new SysUserLoginKey().setUserId(sysUser.getId()).setLoginKey(wechatInfoDTO.getOpenId()), clientDetails.getClientId());

        //缓存用户名
        //        sysUserNameCacheManager.put(sysUser.getId(), sysUser.getUsername());
        return sysUser;
    }

    @Transactional
    @Override
    public void bindingRole(String userId, List<BindingRoleRequest> createRequest) {
        //校验
        SysUser sysUser = sysUserView.findById(userId);
        AssertUtils.isTrue(ObjUtil.isNotEmpty(sysUser), "用户不存在");

        //清除现有关系
        sysUserRoleView.deleteByUserId(userId);

        //添加新的关系
        createRequest.forEach(request -> {
            SysRole role = sysRoleView.findById(request.getRoleId(), true);
            AssertUtils.isTrue(ObjUtil.isNotEmpty(role), "角色不存在");

            SysUserRole userRole = new SysUserRole().setRoleId(request.getRoleId()).setUserId(userId);
            sysUserRoleView.saveOrUpdate(userRole);
        });
        loginService.logout(userId);
    }

    @Transactional
    @Override
    public void bindingPhone(BindingPhoneRequest request) {

        smsService.checkSms(request.getPhone(), request.getCode(), request.getSendType());

        //添加登录关系
        SysUser newUser = sysUserView.findOne(new FindOneSysUserRequest().setPhone(request.getPhone()));
        AssertUtils.notEmpty(newUser, "未授权手机账号");

        //获取旧账号信息
        String userId = LoginUserUtils.getUserTemporary().getUserId();
        SysUser old = sysUserView.findById(userId);
        AssertUtils.notEmpty(old, "旧账号不存在");

        //删除旧账号，并且清除关系
        deleteByIds(Collections.singletonList(old.getId()));

        String openId = LoginUserUtils.getUserTemporary().getLoginKey();
        String clientId = LoginUserUtils.getUserTemporary().getClientId();

        sysUserLoginKeyView.save(new SysUserLoginKey().setUserId(newUser.getId()).setLoginKey(openId), clientId);

    }

    @Override
    public String updateById(UpdateSysUserRequest updateRequest) {
        var entity = BeanUtils.copyProperties(updateRequest, SysUser.class);

        SysUser byId = sysUserView.findById(updateRequest.getId());
        AssertUtils.notEmpty(byId, "用户不存在");

        if (ObjUtil.isNotEmpty(entity.getPhone())) {
            SysUser one = sysUserView.findOne(new FindOneSysUserRequest().setPhone(entity.getPhone()), new FindOneSysUserRequest().setId(entity.getId()));
            AssertUtils.isTrue(one == null, "手机号已存在");
        }

        //        AssertUtils.isTrue(byId.getUsername().equals(updateRequest.getUsername()), "不能修改账号");

        //更新密码
        if (ObjUtil.isNotEmpty(updateRequest.getPasswordNew())) {
            AssertUtils.notEmpty(updateRequest.getPasswordOld(), "旧密码不能为空");

            AssertUtils.isTrue(CryptoUtil.checkPassword(updateRequest.getPasswordOld(), byId.getPassword()), "旧密码验证失败");
            String bcrypt = CryptoUtil.hashPassword(updateRequest.getPasswordNew());
            entity.setPassword(bcrypt);

            loginService.logout(updateRequest.getId());
        }

        //禁用的时候，退出登录
        if (updateRequest.getIsEnabled() == 0 || updateRequest.getIsAccountNonLocked() == 0 || updateRequest.getIsAccountNonExpired() == 0
            || updateRequest.getIsCredentialsNonExpired() == 0) {
            loginService.logout(updateRequest.getId());
        }

        //修改用户信息
        sysUserView.saveOrUpdate(entity);

        //更新角色信息
        if (ObjUtil.isNotEmpty(updateRequest.getRoleIds())) {
            bindingRole(entity.getId(), updateRequest.getRoleIds());
        }

        //        sysUserNameCacheManager.put(entity.getId(), entity.getNickname());
        return entity.getId();
    }

    @Override
    public void resetPwd(RestSysUserRequest resetPwdRequest) {
        SysUser byId = sysUserView.findById(resetPwdRequest.getId());
        AssertUtils.notEmpty(byId, "用户不存在");
        byId.setPassword(CryptoUtil.hashPassword(resetPwdRequest.getPassword()));
        sysUserView.saveOrUpdate(byId);

        loginService.logout(resetPwdRequest.getId());
    }

    @Transactional
    @Override
    public void deleteByIds(List<String> ids) {
        sysUserView.deleteByIds(ids);

        ids.forEach(s -> {
            sysUserRoleView.deleteByUserId(s);
            sysUserLoginKeyView.deleteByUserId(s);
            loginService.logout(s);
        });
    }

    @Override
    public void disarmAccount() {
        String userId = LoginUserUtils.getUserTemporary().getUserId();
        sysUserLoginKeyView.deleteByUserId(userId);
        loginService.logout(userId);
    }

    @Override
    public void bindingAccount(BindingAccountRequest request) {

        //校验用户身份
        FindOneSysUserRequest findOneSysUserRequest = new FindOneSysUserRequest();
        findOneSysUserRequest.setUsername(request.getUsername());
        SysUser sysUser = sysUserView.findOne(findOneSysUserRequest);
        AssertUtils.notEmpty(sysUser, "账号密码错误");

        AssertUtils.isTrue(CryptoUtil.checkPassword(request.getPassword(), sysUser.getPassword()), "账号密码错误");

        //添加登录关系
        SysUser newUser = sysUserView.findOne(new FindOneSysUserRequest().setUsername(request.getUsername()));
        AssertUtils.notEmpty(newUser, "未授权账号");

        //获取旧账号信息
        String userId = LoginUserUtils.getUserTemporary().getUserId();
        SysUser old = sysUserView.findById(userId);
        AssertUtils.notEmpty(old, "旧账号不存在");

        //删除旧账号，并且清除关系
        deleteByIds(Collections.singletonList(old.getId()));

        String openId = LoginUserUtils.getUserTemporary().getLoginKey();
        String clientId = LoginUserUtils.getUserTemporary().getClientId();

        sysUserLoginKeyView.save(new SysUserLoginKey().setUserId(newUser.getId()).setLoginKey(openId), clientId);
    }
}