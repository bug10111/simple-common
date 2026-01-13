package com.simple.common.auth.client.common.process;

import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.entity.login.UserTemporary;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.enums.process.AuthInterceptorKindProcess;
import com.simple.common.auth.client.common.manager.token.TokenManager;
import com.simple.common.auth.client.common.manager.user.LoginInfoManager;
import com.simple.common.auth.client.util.LoginInfoManagerUtils;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.common.enums.process.DefaultKindProcess;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Component
public class CheckTokenAuthProcess implements AuthProcess {

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Override
    public DefaultKindProcess getProcess() {
        return AuthInterceptorKindProcess.CHECK_TOKEN;
    }

    @Override
    public void execute(HttpServletRequest request, HttpServletResponse response, String token, String path, String ipAddr) {
        Map<String, Object> payload = tokenManager.check(token, false);
        LoginInfoManager loginInfoManager;
        if (clientAuthInfo.getClient()) {
            loginInfoManager = LoginInfoManagerUtils.getCliLoginInfoManager();
        } else {
            loginInfoManager = LoginInfoManagerUtils.getSerLoginInfoManager();
        }
        Map<Object, Object> userInfo = loginInfoManager.getUserInfo(payload.get(TokenConstant.jtiKey).toString());
        AssertUtils.notEmpty(userInfo, LoginException.LOGIN_EXPIRED);

        UserTemporary userTemporary = new UserTemporary();
        userTemporary.setUserId((String) userInfo.get(TokenConstant.userIdKey));
        userTemporary.setNickname((String) userInfo.get(TokenConstant.nicknameKey));
        userTemporary.setLoginKey((String) userInfo.get(TokenConstant.loginKey));
        userTemporary.setJti((String) payload.get(TokenConstant.jtiKey));
        userTemporary.setPath(path);
        userTemporary.setClientId((String) userInfo.get(TokenConstant.clientIdKey));
        userTemporary.setClientName((String) userInfo.get(TokenConstant.clientNameKey));
        userTemporary.setAppNames((String) userInfo.get(TokenConstant.appNamesKey));
        userTemporary.setWxAppId((String) userInfo.get(TokenConstant.wxAppIdKey));
        userTemporary.setScopes(JsonUtils.toList(userInfo.get(TokenConstant.scopesKey).toString(), String.class));
        userTemporary.setLoginRole(JsonUtils.toList(userInfo.get(TokenConstant.loginRole).toString(), String.class));
        userTemporary.setExtension(userInfo.get(TokenConstant.extensionKey));
        LoginUserUtils.add(userTemporary);
    }
}
