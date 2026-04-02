package com.simple.oauth.manager.login;

import com.simple.common.auth.server.common.entity.AbsUserDetails;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.core.utils.CryptoUtil;
import com.simple.common.core.utils.AssertUtils;
import com.simple.oauth.common.dto.sysUser.FindOneSysUserRequest;
import com.simple.oauth.common.entity.login.PwdLoginRequest;
import com.simple.oauth.common.entity.sysUser.SysUser;
import com.simple.oauth.common.view.sysUser.SysUserView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 账号密码登录实现
 *
 * @author 兄台丶请冷静
 */
@Component
public class PwdLoginManager extends OauthAbsLoginManager {

    @Autowired
    private SysUserView sysUserView;

    @Override
    public boolean support(Object adapter) {
        return adapter instanceof PwdLoginRequest;
    }

    @Override
    public AbsUserDetails login(ClientDetails clientDetails, Object adapter) {
        PwdLoginRequest loginRequest = (PwdLoginRequest) adapter;

        //校验用户身份
        FindOneSysUserRequest findOneSysUserRequest = new FindOneSysUserRequest();
        findOneSysUserRequest.setUsername(loginRequest.getUsername());
        SysUser sysUser = sysUserView.findOne(findOneSysUserRequest);
        AssertUtils.notEmpty(sysUser, "账号密码错误");

        checkErrorNum(clientDetails, loginRequest);
        boolean b = CryptoUtil.checkPassword(loginRequest.getPassword(), sysUser.getPassword());
        if (!b) {
            loginError(clientDetails, loginRequest);
        }
        return collectInformation(sysUser,clientDetails , null);
    }
}
