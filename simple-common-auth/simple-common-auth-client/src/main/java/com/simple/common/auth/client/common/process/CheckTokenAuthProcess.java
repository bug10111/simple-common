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
import com.simple.common.core.utils.BeanUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.core.utils.SignUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
@Slf4j
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
        String encoded = request.getHeader(TokenConstant.userHead);
        String sign = request.getHeader(TokenConstant.userSignHead);
        // 密钥未加载时 getKey 抛出 IllegalStateException（部署错误必须显式失败，不允许未校验透传）
        String key = signManager.getKey();

        // 内部透传快速通道：恒定时间验签（防时序侧信道），解码/解析失败一律回落到常规 Token 校验（fail-closed）
        if (ObjUtil.isNotEmpty(encoded) && ObjUtil.isNotEmpty(sign)) {
            if (SignUtils.verifyWeb(encoded, sign, key)) {
                try {
                    byte[] decoded = Base64.getDecoder().decode(encoded);
                    String userJson = new String(decoded, StandardCharsets.UTF_8);
                    UserTemporary user = JsonUtils.toJsonObj(userJson, UserTemporary.class);
                    // 仅在拿到合法用户对象时才短路；解析失败回落常规校验，不允许 null 进入登录上下文
                    if (user != null && ObjUtil.isNotEmpty(user.getUserId())) {
                        LoginUserUtils.add(user);
                        return;
                    }
                    log.warn("内部透传用户上下文不完整，回落到常规 Token 校验");
                } catch (RuntimeException e) {
                    log.warn("内部透传用户上下文解析失败，回落到常规 Token 校验", e);
                }
            }
        }

        Map<String, Object> payload = tokenManager.check(token, false);
        LoginInfoManager loginInfoManager;
        if (clientAuthInfo.getClient()) {
            loginInfoManager = LoginInfoManagerUtils.getClientLoginInfoManager();
        } else {
            loginInfoManager = LoginInfoManagerUtils.getServerLoginInfoManager();
        }
        AssertUtils.notNull(loginInfoManager, "LoginInfoManager 未正确初始化");

        // jti 单次提取并校验（载荷可能不含 jti，或载荷缺失被实现方容忍）
        Object jtiObj = payload.get(TokenConstant.jtiKey);
        AssertUtils.notEmpty(jtiObj, LoginException.LOGIN_EXPIRED, "Token载荷缺少 jti");
        String jti = jtiObj.toString();

        Map<Object, Object> userInfo = loginInfoManager.getUserInfo(jti);
        AssertUtils.notEmpty(userInfo, LoginException.LOGIN_EXPIRED);

        UserTemporary userTemporary = BeanUtils.fillBeanWithMap(userInfo, UserTemporary.class);
        userTemporary.setJti(jti);
        userTemporary.setPath(path);

        // 权限范围（List 元素逐个 String 化，缓存反序列化出 Integer 等类型时不产生 ClassCastException）
        if (ObjUtil.isNotEmpty(userInfo.get(TokenConstant.scopesKey))) {
            Object scopesObj = userInfo.get(TokenConstant.scopesKey);
            List<String> scopes;
            if (scopesObj instanceof Collection) {
                // 本地缓存或直接对象：已经是集合
                scopes = ((Collection<?>) scopesObj).stream().map(String::valueOf).collect(Collectors.toList());
            } else {
                // Redis 缓存：JSON 字符串格式 ["ALL"]
                scopes = JsonUtils.toList(scopesObj.toString(), String.class);
            }
            if (scopes != null) {
                userTemporary.setScopes(scopes);
            }
        }

        // 登录角色（Collection 与 JSON 字符串双形态兼容；元素逐个 String 化）
        if (ObjUtil.isNotEmpty(userInfo.get(TokenConstant.loginRole))) {
            Object loginRoleObj = userInfo.get(TokenConstant.loginRole);
            List<String> loginRole;
            if (loginRoleObj instanceof Collection) {
                // 本地缓存或直接对象：已经是集合
                loginRole = ((Collection<?>) loginRoleObj).stream().map(String::valueOf).collect(Collectors.toList());
            } else {
                // Redis 缓存：JSON 字符串格式 ["admin","user"]
                loginRole = JsonUtils.toList(loginRoleObj.toString(), String.class);
            }
            if (loginRole != null) {
                userTemporary.setLoginRole(loginRole);
            }
        }

        // 数据权限（Local 实现缓存的是对象、Redis 实现缓存的是 JSON 字符串：按形态分派，避免 toString 出 {tenantId=1} 伪 JSON）
        Object dataPermissionObj = userInfo.get(TokenConstant.dataPermissionKey);
        if (dataPermissionObj != null) {
            DataPermission dataPermission;
            if (dataPermissionObj instanceof DataPermission) {
                dataPermission = (DataPermission) dataPermissionObj;
            } else if (dataPermissionObj instanceof Map) {
                dataPermission = BeanUtils.fillBeanWithMap((Map<?, ?>) dataPermissionObj, DataPermission.class);
            } else {
                dataPermission = JsonUtils.toJsonObj(dataPermissionObj.toString(), DataPermission.class);
            }
            if (dataPermission != null) {
                userTemporary.setDataPermission(dataPermission);
            } else {
                log.warn("用户 dataPermission 解析结果为空，本次请求未加载数据权限：userId={}", userTemporary.getUserId());
            }
        }

        LoginUserUtils.add(userTemporary);
    }
}
