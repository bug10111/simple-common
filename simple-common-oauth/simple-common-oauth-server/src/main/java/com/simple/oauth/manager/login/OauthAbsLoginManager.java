package com.simple.oauth.manager.login;

import com.simple.common.auth.server.common.entity.AbsUserDetails;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.manager.login.AbsLoginManager;
import com.simple.common.core.utils.BeanUtils;
import com.simple.oauth.common.dto.sysRole.SysRoleInfoResponse;
import com.simple.oauth.common.entity.sysUser.SysUser;
import com.simple.oauth.common.service.sysMenu.SysMenuService;
import com.simple.oauth.common.view.sysRole.SysRoleView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
public abstract class OauthAbsLoginManager extends AbsLoginManager {

    @Autowired
    private SysRoleView sysRoleView;

    @Autowired
    private SysMenuService sysMenuService;

    /**
     * 收集用户信息
     *
     * @param sysUser       登录用户
     * @param clientDetails 客户端信息
     * @param loginKey      第三方登录标志
     */
    protected AbsUserDetails collectInformation(SysUser sysUser, ClientDetails clientDetails, String loginKey) {

        //收集登录信息
        AbsUserDetails absUserDetails = BeanUtils.copyProperties(sysUser, AbsUserDetails.class);
        absUserDetails.setUserId(sysUser.getId());
        absUserDetails.setNickname(sysUser.getNickname());
        absUserDetails.setLoginKey(loginKey);

        //收集角色信息
        List<SysRoleInfoResponse> roles = sysRoleView.findByUserIdAndServer(sysUser.getId());
        Set<String> list = roles.stream().map(SysRoleInfoResponse::getRoleKey).collect(Collectors.toSet());
        absUserDetails.setLoginRole(list);
        return absUserDetails;
    }
}
