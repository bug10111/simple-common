package com.simple.common.auth.client.common.process;

import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.entity.login.DataPermission;
import com.simple.common.auth.client.common.entity.login.UserTemporary;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.enums.process.AuthInterceptorKindProcess;
import com.simple.common.auth.client.common.manager.sign.SignManager;
import com.simple.common.auth.client.common.manager.token.TokenManager;
import com.simple.common.auth.client.common.manager.user.LoginInfoManager;
import com.simple.common.auth.client.util.LoginInfoManagerUtils;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.common.enums.process.DefaultKindProcess;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.core.utils.SignUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Token 合法性校验处理器。
 * <p>
 * <b>【重要安全提醒】</b>：此处理器包含一个内部服务间用户信息透传的机制。
 * 当请求来自可信网关（如Spring Cloud Gateway）时，允许通过请求头 {@code X-User-Context} 和 {@code X-User-Signature}
 * 直接传递用户信息，从而绕过JWT解析，提升性能。
 * <p>
 * 此机制的启用与否以及信任来源的判断逻辑，<b>必须由集成方根据自身部署架构来实现</b>。
 * 当前代码中的内部头校验部分是开放的，集成方需：
 * <ol>
 *   <li>替换 {@code SignManager} 为共享密钥实现，确保网关与微服务使用同一密钥。</li>
 * </ol>
 * 若未正确实施安全策略而直接使用，存在认证绕过风险。
 *
 * @author qty
 */
@Component
public class CheckTokenAuthProcess implements AuthProcess {

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Autowired
    private SignManager signManager;

    @Override
    public DefaultKindProcess getProcess() {
        return AuthInterceptorKindProcess.CHECK_TOKEN;
    }

    @Override
    public void execute(HttpServletRequest request, HttpServletResponse response, String token, String path, String ipAddr) {
        // 检查内部传递头（服务间透传用户信息）
        String encoded = request.getHeader(TokenConstant.userHead);
        String sign = request.getHeader(TokenConstant.userSignHead);
        String key = signManager.getKey();

        if (ObjUtil.isNotEmpty(encoded) && ObjUtil.isNotEmpty(sign)) {
            String signNew = SignUtils.signWeb(encoded, key);
            if (sign.equals(signNew)) {
                byte[] decoded = Base64.getDecoder().decode(encoded);
                String userJson = new String(decoded, StandardCharsets.UTF_8);
                UserTemporary user = JsonUtils.toJsonObj(userJson, UserTemporary.class);
                LoginUserUtils.add(user);
                return;
            }
        }

        // 正常 token 校验
        Map<String, Object> payload = tokenManager.check(token, false);
        LoginInfoManager loginInfoManager;
        if (clientAuthInfo.getClient()) {
            loginInfoManager = LoginInfoManagerUtils.getCliLoginInfoManager();
        } else {
            loginInfoManager = LoginInfoManagerUtils.getSerLoginInfoManager();
        }
        AssertUtils.notNull(loginInfoManager, "LoginInfoManager 未正确初始化");

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
        // 解析数据权限
        Object dataPermissionObj = userInfo.get(TokenConstant.dataPermissionKey);
        if (dataPermissionObj != null) {
            DataPermission dataPermission = JsonUtils.toJsonObj(dataPermissionObj.toString(), DataPermission.class);
            userTemporary.setDataPermission(dataPermission);
        }
        LoginUserUtils.add(userTemporary);
    }
}