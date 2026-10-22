package com.simple.common.auth.client.common.process;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.enums.process.AuthInterceptorKindProcess;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.common.enums.process.DefaultKindProcess;
import com.simple.common.core.utils.AssertUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 授权范围校验处理器。
 * <p>
 * 语义契约：本服务声明的 scope（ClientAuthInfo#addScope）为「访问本服务所需的全部权限范围」，
 * 用户 scopes 必须 <b>包含全部</b> 所需 scope 才放行（containsAll）；
 * 原实现 containsAny 只需命中一个即可通过，部分授权的 token 即可绕过本服务的范围要求（越权放大）。
 * 用户 scopes 缺失按无权限处理（fail-closed）。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class CheckScopeAuthProcess implements AuthProcess {

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Override
    public DefaultKindProcess getProcess() {
        return AuthInterceptorKindProcess.CHECK_SCOPE_AUTH;
    }

    @Override
    public void execute(HttpServletRequest request, HttpServletResponse response,
                        String token, String path, String ipAddr) {
        // 需要鉴权
        if (clientAuthInfo.getAuthentication()) {
            Set<String> requiredScopes = clientAuthInfo.getScope();
            Set<String> userScopes = LoginUserUtils.getUserTemporary().getScopes();

            // 用户 scopes 缺失/不完整 = 授权范围不足，拒绝（fail-closed）
            boolean pass = userScopes != null && userScopes.containsAll(requiredScopes);
            if (!pass) {
                log.error("URL==>[{}]请求被拦截！授权范围不足，需要 {}，实际 {}", path, requiredScopes, userScopes);
                AssertUtils.error(LoginException.INSUFFICIENT_PERMISSIONS, "URL==>[{}]请求被拦截！授权范围权限不足！", path);
            }
        }
    }
}
