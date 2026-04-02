package com.simple.oauth.manager.init;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import com.simple.common.core.common.properties.ApplicationProperties;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.Base64Utils;
import com.simple.common.core.utils.CryptoUtil;
import com.simple.oauth.common.dto.sysRole.FindOneSysRoleRequest;
import com.simple.oauth.common.dto.sysUser.FindOneSysUserRequest;
import com.simple.oauth.common.entity.sysClientDetails.SysClientDetails;
import com.simple.oauth.common.entity.sysRole.SysRole;
import com.simple.oauth.common.entity.sysUser.SysUser;
import com.simple.oauth.common.entity.sysUserRole.SysUserRole;
import com.simple.oauth.common.enums.RoleType;
import com.simple.oauth.common.enums.ServerType;
import com.simple.oauth.common.manager.init.OauthInitManager;
import com.simple.oauth.common.manager.role.RoleAuthCacheManager;
import com.simple.oauth.common.properties.OauthProperties;
import com.simple.oauth.common.view.sysClientDetails.SysClientDetailsView;
import com.simple.oauth.common.view.sysRole.SysRoleView;
import com.simple.oauth.common.view.sysUser.SysUserView;
import com.simple.oauth.common.view.sysUserRole.SysUserRoleView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Component
public class DefaultOauthInitManager implements OauthInitManager {

    @Autowired
    private OauthProperties oauthProperties;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private SysClientDetailsView sysClientDetailsView;

    @Autowired
    private SysUserView sysUserView;

    @Autowired
    private SysRoleView sysRoleView;

    @Autowired
    private SysUserRoleView sysUserRoleView;

    @Autowired
    private RoleAuthCacheManager roleAuthCacheManager;

    @Override
    public void loadingSysConfig() {

    }

    @Override
    @Transactional
    public void loadingSecret() {

//        //加载授权中心服务端
//        SysClientDetails server = sysClientDetailsView.findAllByClientId(oauthProperties.getOauth());
//        if (ObjUtil.isEmpty(server)) {
//            server = new SysClientDetails();
//            server.setServer(applicationProperties.getName());
//            server.setServerType(ServerType.server);
//            server.setClientName(oauthProperties.getOauth());
//            server.setClientId(oauthProperties.getOauth());
//            sysClientDetailsView.save(server);
//            server.setRemark("系统自动创建的授权中心客户端");
//        }

        //加载授权中心客户端
        SysClientDetails sysClientDetails = sysClientDetailsView.findAllByClientId(oauthProperties.getOauthClient());
        if (ObjUtil.isEmpty(sysClientDetails)) {
            sysClientDetails = new SysClientDetails();
            // 生成RSA密钥对
            java.security.KeyPair keyPair = CryptoUtil.generateKeyPair(CryptoUtil.AsymmetricAlgorithmType.RSA_OAEP);
            sysClientDetails.setRsaPublic(Base64Utils.encode(keyPair.getPublic().getEncoded()));
            sysClientDetails.setRsaPrivate(Base64Utils.encode(keyPair.getPrivate().getEncoded()));
            // 生成AES密钥
            byte[] aesKey = CryptoUtil.generateSymmetricKey(CryptoUtil.SymmetricAlgorithmType.AES_GCM);
            sysClientDetails.setHsKey(Base64Utils.encode(aesKey));

            sysClientDetails.setServer(applicationProperties.getName());
            sysClientDetails.setServerType(ServerType.client);
            sysClientDetails.setClientName(oauthProperties.getOauthClient());
            sysClientDetails.setScope(Collections.singletonList(applicationProperties.getName()));
            sysClientDetails.setClientId(oauthProperties.getOauthClient());
            sysClientDetails.setRemark("系统自动创建的授权中心客户端");
            sysClientDetails.setClientSecret(oauthProperties.getOauthPwd());
            sysClientDetailsView.saveAndLoading(sysClientDetails);
            log.debug("授权中心未检测到有效初始客户端，创建新的初始客户端");
        }

        //加载客户端密钥
        List<SysClientDetails> all = sysClientDetailsView.findAll(ServerType.client);
        if (ObjUtil.isNotEmpty(all)) {
            all.forEach(sysClientDetailsView::loading);
        }

        log.debug("客户端密钥信息加载完毕");
    }

    @Override
    @Transactional
    public void loadingUserAndRole() {

        //加载默认角色
        SysRole role = sysRoleView.findOne(new FindOneSysRoleRequest().setRoleKey(oauthProperties.getAdmin()), true);
        if (ObjUtil.isEmpty(role)) {
            role = new SysRole();
            role.setRoleKey(oauthProperties.getAdmin());
            role.setRoleName(oauthProperties.getAdmin());
            role.setServer(applicationProperties.getName());
            role.setType(RoleType.SERVER);
            role.setRemark("默认超级管理员角色，拥有管理后台所有权限");
            sysRoleView.saveOrUpdate(role);
        }

        //加载默认角色
        SysRole tourists = sysRoleView.findOne(new FindOneSysRoleRequest().setRoleKey(oauthProperties.getTourists()), true);
        if (ObjUtil.isEmpty(tourists)) {
            tourists = new SysRole();
            tourists.setRoleKey(oauthProperties.getTourists());
            tourists.setRoleName(oauthProperties.getTourists());
            tourists.setServer(applicationProperties.getName());
            tourists.setType(RoleType.SERVER);
            tourists.setRemark("默认游客角色，可以登录小程序访问部分内容");
            sysRoleView.saveOrUpdate(tourists);
        }

        SysUser user = sysUserView.findOne(new FindOneSysUserRequest().setUsername(oauthProperties.getAdmin()));
        if (ObjUtil.isEmpty(user)) {
            user = new SysUser();
            user.setUsername(oauthProperties.getAdmin());
            user.setPassword(CryptoUtil.hashPassword(oauthProperties.getAdminPwd()));
            user.setIsAccountNonExpired(1);
            user.setIsEnabled(1);
            user.setIsAccountNonLocked(1);
            user.setIsCredentialsNonExpired(1);
            sysUserView.saveOrUpdate(user);

            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            sysUserRoleView.saveOrUpdate(userRole);
            log.debug("授权中心未检测到超级管理员账号，创建新的超级管理员账号");
        }
    }

    @Override
    public void auth() {
        roleAuthCacheManager.update();
    }
}