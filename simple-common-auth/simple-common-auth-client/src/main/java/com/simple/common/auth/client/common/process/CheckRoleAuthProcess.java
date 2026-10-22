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

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA
 * Description: 基于 URL 的角色权限校验处理器
 * <p>
 * 语义契约（两段校验全部执行，多限制取交集，fail-closed）：
 * <ol>
 *   <li>URL→角色限制（onlyRole）：命中路径的规则中任一规则允许当前用户角色即通过；</li>
 *   <li>角色→URL限制（roleByRestricted）：用户任一受限角色的允许路径集合不包含当前路径即拒绝。</li>
 * </ol>
 * 两段独立生效：一段通过不会短路另一段（最严限制优先）。
 * 多个规则同时命中同一路径时按最长模式优先（最具体规则语义）。
 * </p>
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
        Set<String> userRoles = LoginUserUtils.getUserTemporary().getLoginRole();
        if (userRoles == null || userRoles.isEmpty()) {
            // 没有角色直接拒绝（fail-closed：openAuthentication 开启时无角色用户不允许访问受控接口；
            // 完全公开路径应配置到白名单，而不是依赖角色校验放行）
            AssertUtils.error(LoginException.INSUFFICIENT_PERMISSIONS, "用户未分配任何角色，无法访问 [{}]", path);
        }

        //先检查 onlyRole 配置（URL 只允许特定角色）
        Map<String, Set<String>> roleMap = clientAuthInfo.getRoleMap();
        if (!roleMap.isEmpty()) {

            // 找到匹配当前 URL 的配置（多个模式同时命中时取最长模式，保证最具体规则语义且结果确定）
            String matchedUrl = roleMap.keySet().stream()
                    .filter(pattern -> UrlRulesUtils.isMatch(pattern, path))
                    .max(Comparator.comparingInt(String::length))
                    .orElse(null);
            if (matchedUrl != null) {
                Set<String> allowedRoles = roleMap.get(matchedUrl);

                // 判断用户角色与允许角色是否有交集
                if (!CollectionUtils.matches(userRoles, allowedRoles)) {
                    log.error("URL [{}] 只允许角色 {} 访问，但用户角色为 {}", path, allowedRoles, userRoles);
                    AssertUtils.error(LoginException.INSUFFICIENT_PERMISSIONS, "权限不足，您无权访问此资源");
                }
                // 注意：此段通过不短路 byRoleMap 段——两段限制独立生效（任一段拒绝即拒绝）
            }
        }

        //再检查 byRoleMap 配置（角色只能访问特定 URL）
        Map<String, Set<String>> byRoleMap = clientAuthInfo.getByRoleMap();
        if (!byRoleMap.isEmpty()) {

            // 遍历用户角色，检查是否有角色被限制了可访问的 URL 集合
            // （userRoles 已在上方断言非空且非空集合，AssertUtils.error 失败即抛出，此处无需再判空）
            for (String role : userRoles) {
                Set<String> allowedUrls = byRoleMap.get(role);
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
