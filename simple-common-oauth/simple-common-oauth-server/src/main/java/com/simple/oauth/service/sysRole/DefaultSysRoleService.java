package com.simple.oauth.service.sysRole;

import com.simple.common.core.utils.BeanUtils;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.IdUtils;
import com.simple.oauth.common.dto.sysRole.*;
import com.simple.oauth.common.entity.sysClientDetails.SysClientDetails;
import com.simple.oauth.common.entity.sysRole.SysRole;
import com.simple.oauth.common.entity.sysRoleMenu.SysRoleMenu;
import com.simple.oauth.common.entity.sysUserRole.SysUserRole;
import com.simple.oauth.common.enums.RoleType;
import com.simple.oauth.common.manager.role.RoleAuthCacheManager;
import com.simple.oauth.common.properties.OauthProperties;
import com.simple.oauth.common.service.sysRole.SysRoleService;
import com.simple.oauth.common.view.sysClientDetails.SysClientDetailsView;
import com.simple.oauth.common.view.sysRole.SysRoleView;
import com.simple.oauth.common.view.sysRoleMenu.SysRoleMenuView;
import com.simple.oauth.common.view.sysUserRole.SysUserRoleView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色信息(sys_role)默认接口实现
 *
 * @author qty
 */
@Service
@Transactional
class DefaultSysRoleService implements SysRoleService {

    @Autowired
    private SysRoleView sysRoleView;

    @Autowired
    private SysRoleMenuView sysRoleMenuView;

    @Autowired
    private OauthProperties oauthProperties;

    @Autowired
    private SysUserRoleView sysUserRoleView;

    @Autowired
    private RoleAuthCacheManager roleAuthCacheManager;

    @Autowired
    private SysClientDetailsView sysClientDetailsView;

    @Override
    public IPage<SysRolePageResponse> findAll(FindAllSysRoleRequest findAllRequest) {

        String clientId = LoginUserUtils.getUserTemporary().getClientId();
        SysClientDetails sysClientDetails = sysClientDetailsView.findByClientId(clientId);

        //当前登录的服务是授权服务，显示所有角色
        if (sysClientDetails.getServer().startsWith("oauth")) {
            findAllRequest.setServer(null);
        }

        //不是授权服务，只显示服务拥有的角色
        else {
            findAllRequest.getServer().add(sysClientDetails.getServer());
        }

        var pageInfo = sysRoleView.findAll(findAllRequest);
        return pageInfo.convert(entity -> BeanUtils.copyProperties(entity, SysRolePageResponse.class));
    }

    @Override
    public List<SysRolePageResponse> listbyUser(FindAllSysRoleRequest findAllRequest) {
        String userId = LoginUserUtils.getUserTemporary().getUserId();

        String clientId = LoginUserUtils.getUserTemporary().getClientId();
        SysClientDetails sysClientDetails = sysClientDetailsView.findByClientId(clientId);
        String server = "";

        //当前登录的服务是授权服务，显示所有角色
        if (sysClientDetails.getServer().startsWith("oauth")) {
            server = null;
        }

        //不是授权服务，只显示服务拥有的角色
        else {
            server = sysClientDetails.getServer();
        }

        sysRoleView.findByUserIdAndServer(userId, server);
        return List.of();
    }

    @Override
    public List<SysRoleInfoResponse> getRole(String userId) {
        return sysRoleView.findByUserIdAndServer(userId);
    }

    @Override
    public SysRoleInfoResponse findById(String id) {
        var sysRole = sysRoleView.findById(id, false);
        return BeanUtils.copyProperties(sysRole, SysRoleInfoResponse.class);
    }

    @Transactional
    @Override
    public String save(CreateSysRoleRequest createRequest) {
        var entity = BeanUtils.copyProperties(createRequest, SysRole.class);
        AssertUtils.isTrue((createRequest.getType() == RoleType.SERVER) || ObjUtil.isEmpty(createRequest.getSysMenuIds()), "客户端不能分配权限");

        //添加角色
        SysRole one = sysRoleView.findOne(new FindOneSysRoleRequest().setRoleName(createRequest.getRoleName()), true);
        AssertUtils.isTrue(one == null, "角色名称已存在");

        SysRole key = sysRoleView.findOne(new FindOneSysRoleRequest().setRoleKey(createRequest.getRoleKey()), true);
        AssertUtils.isTrue(key == null, "角色key已存在");
        sysRoleView.saveOrUpdate(entity);

        //添加权限信息
        return saveRoleMenu(entity, createRequest.getSysMenuIds());
    }

    @Transactional
    @Override
    public String updateById(UpdateSysRoleRequest updateRequest) {
        SysRole byId = sysRoleView.findById(updateRequest.getId(), false);
        var entity = BeanUtils.copyProperties(updateRequest, SysRole.class);

        FindOneSysRoleRequest ne = new FindOneSysRoleRequest().setId(entity.getId());
        SysRole one = sysRoleView.findOne(new FindOneSysRoleRequest().setRoleName(entity.getRoleName()), ne, true);
        AssertUtils.isTrue(one == null, "角色名称已存在");

        //超级管理员不能修改
        if (byId.getRoleKey().equals(oauthProperties.getAdmin())) {
            if (!entity.getRoleKey().equals(oauthProperties.getAdmin())) {
                AssertUtils.error("超级管理员不能修改key");
            }
        }

        SysRole key = sysRoleView.findOne(new FindOneSysRoleRequest().setRoleKey(entity.getRoleKey()), ne, true);
        AssertUtils.isTrue(key == null, "角色key已存在");
        sysRoleView.saveOrUpdate(entity);

        //清空现有的权限
        sysRoleMenuView.deleteByRoleId(entity.getId());

        //添加权限信息
        return saveRoleMenu(entity, updateRequest.getSysMenuIds());
    }

    /**
     * 添加角色权限信息
     *
     * @param entity     角色信息
     * @param sysMenuIds 权限信息
     */
    protected String saveRoleMenu(SysRole entity, List<String> sysMenuIds) {
        if (ObjUtil.isNotEmpty(sysMenuIds)) {
            List<SysRoleMenu> list = new ArrayList<>();
            sysMenuIds.forEach(s -> {
                DateTime now = DateTime.now();
                SysRoleMenu sysRoleMenu = new SysRoleMenu().setMenuId(s).setRoleId(entity.getId()).setId(IdUtils.getFastSimpleUUID()).setCreateTime(now).setUpdateTime(now);
                list.add(sysRoleMenu);
            });
            sysRoleMenuView.saves(list);

            roleAuthCacheManager.put(entity.getRoleKey(), sysMenuIds);
        }
        return entity.getId();
    }

    @Override
    @Transactional
    public void deleteByIds(List<String> ids) {
        ids.forEach(s -> {
            List<SysUserRole> byRoleId = sysUserRoleView.findByRoleId(s);
            AssertUtils.isTrue(byRoleId.isEmpty(), "存在绑定用户的角色，不能删除");
        });
        sysRoleView.deleteByIds(ids);
    }
}

