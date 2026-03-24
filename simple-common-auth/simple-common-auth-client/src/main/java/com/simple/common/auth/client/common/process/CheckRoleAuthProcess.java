package com.simple.common.auth.client.common.process;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.enums.process.AuthInterceptorKindProcess;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.common.enums.process.DefaultKindProcess;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.CollectionUtils;
import com.simple.common.core.utils.UrlRulesUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 基于 URL 的角色权限校验处理器
 *
 * @author qty
 */
@Slf4j
@Component
public class CheckRoleAuthProcess implements AuthProcess {

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Override
    public DefaultKindProcess getProcess() {
        return AuthInterceptorKindProcess.CHECK_ROLE;
    }

    @Override
    public void execute(HttpServletRequest request, HttpServletResponse response, String token, String path, String ipAddr) {
        if (!clientAuthInfo.getAuthentication()) {
            return;
        }

        //获取用户角色
        HashSet<String> userRoles = LoginUserUtils.getUserTemporary().getLoginRole();
        if (userRoles == null || userRoles.isEmpty()) {

            // 没有角色，直接拒绝
            AssertUtils.error(LoginException.INSUFFICIENT_PERMISSIONS, "用户未分配任何角色，无法访问 [{}]", path);
        }

        //先检查 onlyRole 配置（URL 只允许特定角色）
        Map<String, HashSet<String>> roleMap = clientAuthInfo.getRoleMap();
        if (!roleMap.isEmpty()) {

            // 找到匹配当前 URL 的配置
            String matchedUrl = UrlRulesUtils.findMatch(path, roleMap.keySet());
            if (matchedUrl != null) {
                HashSet<String> allowedRoles = roleMap.get(matchedUrl);

                // 判断用户角色与允许角色是否有交集
                if (!CollectionUtils.matches(userRoles, allowedRoles)) {
                    log.error("URL [{}] 只允许角色 {} 访问，但用户角色为 {}", path, allowedRoles, userRoles);
                    AssertUtils.error(LoginException.INSUFFICIENT_PERMISSIONS, "权限不足，您无权访问此资源");
                }
                return;
            }
        }

        //再检查 byRoleMap 配置（角色只能访问特定 URL）
        Map<String, HashSet<String>> byRoleMap = clientAuthInfo.getByRoleMap();
        if (!byRoleMap.isEmpty()) {

            // 遍历用户角色，检查是否有角色被限制了可访问的 URL 集合
            if (userRoles != null) {
                for (String role : userRoles) {
                    HashSet<String> allowedUrls = byRoleMap.get(role);
                    if (allowedUrls != null && !allowedUrls.isEmpty()) {

                        // 如果该角色有 URL 限制，则判断当前 URL 是否在允许列表中
                        if (!UrlRulesUtils.matches(path, allowedUrls)) {
                            log.error("角色 [{}] 只能访问 {}，当前访问 URL [{}] 不在允许范围", role, allowedUrls, path);
                            AssertUtils.error(LoginException.INSUFFICIENT_PERMISSIONS, "权限不足，您无权访问此资源");
                        }
                    }
                }
            }
        }
    }
}
