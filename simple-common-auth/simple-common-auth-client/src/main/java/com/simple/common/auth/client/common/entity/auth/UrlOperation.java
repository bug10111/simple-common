package com.simple.common.auth.client.common.entity.auth;

import com.simple.common.core.utils.AssertUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA
 * URL 访问规则操作器（链式配置入口）。
 *
 * @author qty
 */
public class UrlOperation {

    private final String url;

    private final ClientAuthInfo clientAuthInfo;

    public UrlOperation(ClientAuthInfo clientAuthInfo, String url) {
        this.clientAuthInfo = clientAuthInfo;
        this.url = url;
    }

    /**
     * 放开
     */
    public ClientAuthInfo permitAll() {
        Map<String, Boolean> urlList = clientAuthInfo.getWhiteMap();
        urlList.put(url, true);
        return clientAuthInfo;
    }

    /**
     * 当前路径只允许指定IP访问，其他IP不能访问
     * <p>多次调用对同一 URL 的允许 IP 取并集。</p>
     *
     * @param ips IP
     */
    public ClientAuthInfo onlyIp(String... ips) {
        AssertUtils.isTrue(ips != null && ips.length > 0, "onlyIp 必须至少配置一个IP，否则该路径将被拒绝所有访问");
        Set<String> ipSet = clientAuthInfo.getIpMap().computeIfAbsent(url, k -> new HashSet<>());
        ipSet.addAll(Arrays.asList(ips));
        return clientAuthInfo;
    }

    /**
     * 当前路径只允许指定角色访问，其他角色不能访问
     * <p>多次调用对同一 URL 的允许角色取并集（与 onlyIp 合并语义一致，避免重复声明静默覆盖安全规则）。</p>
     *
     * @param role 角色
     */
    public ClientAuthInfo onlyRole(String... role) {
        AssertUtils.isTrue(role != null && role.length > 0, "onlyRole 必须至少配置一个角色，否则该路径将被拒绝所有访问");
        Set<String> roleSet = clientAuthInfo.getRoleMap().computeIfAbsent(url, k -> new HashSet<>());
        roleSet.addAll(Arrays.asList(role));
        return clientAuthInfo;
    }

    /**
     * 角色只允许通过当前路径，其他角色可以访问
     * <p>多次调用对同一 URL 的受限角色取并集。</p>
     *
     * @param role 角色
     */
    public ClientAuthInfo roleByRestricted(String... role) {
        AssertUtils.isTrue(role != null && role.length > 0, "roleByRestricted 必须至少配置一个角色");
        Set<String> roleSet = clientAuthInfo.getByRoleMap().computeIfAbsent(url, k -> new HashSet<>());
        roleSet.addAll(Arrays.asList(role));
        return clientAuthInfo;
    }
}
