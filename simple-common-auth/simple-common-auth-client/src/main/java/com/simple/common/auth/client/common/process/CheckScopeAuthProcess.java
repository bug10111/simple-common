package com.simple.common.auth.client.common.process;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.enums.process.AuthInterceptorKindProcess;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.common.enums.process.DefaultKindProcess;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.CollectionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;

/**
 * 授权范围校验处理器。
 * <p>
 * 修复：移除重复的 HashSet 导入。
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
            HashSet<String> scopes = LoginUserUtils.getUserTemporary().getScopes();

            // 判断授权范围
            if (!CollectionUtils.matches(clientAuthInfo.getScope(), scopes)) {
                AssertUtils.error(LoginException.INSUFFICIENT_PERMISSIONS,
                                  "授权范围权限不足", "URL==>[{}]请求被拦截！授权范围权限不足！", path);
            }
        }
    }
}