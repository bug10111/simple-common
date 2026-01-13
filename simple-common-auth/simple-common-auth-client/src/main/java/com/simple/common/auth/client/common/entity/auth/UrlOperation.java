package com.simple.common.auth.client.common.entity.auth;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
public class UrlOperation {

    private final String url;

    private final ClientAuthInfo ClientAuthInfo;

    public UrlOperation(ClientAuthInfo ClientAuthInfo, String url) {
        this.ClientAuthInfo = ClientAuthInfo;
        this.url = url;
    }

    /**
     * 放开
     */
    public ClientAuthInfo permitAll() {
        Map<String, Boolean> urlList = ClientAuthInfo.getWhiteMap();
        urlList.put(url, true);
        return ClientAuthInfo;
    }

    /**
     * 当前路径只允许通过的IP,其他IP不能访问
     * @param ips IP
     */
    public ClientAuthInfo onlyIp(String... ips) {
        Map<String, HashSet<String>> urlList = ClientAuthInfo.getIpMap();
        urlList.computeIfAbsent(url, k -> new HashSet<>()).addAll(Arrays.asList(ips));;
        return ClientAuthInfo;
    }

    /**
     * 当前路径只允许通过的角色，其他角色不能访问
     * @param role 角色
     */
    public ClientAuthInfo onlyRole(String... role) {
        List<String> list = Arrays.asList(role);
        Map<String, HashSet<String>> roleMap = ClientAuthInfo.getRoleMap();
        roleMap.put(url, new HashSet<>(list));
        return ClientAuthInfo;
    }

    /**
     * 角色只允许通过当前路径，其他角色可以访问
     * @param role 角色
     */
    public ClientAuthInfo roleByRestricted(String... role) {
        List<String> list = Arrays.asList(role);
        Map<String, HashSet<String>> roleMap = ClientAuthInfo.getByRoleMap();
        roleMap.put(url, new HashSet<>(list));
        return ClientAuthInfo;
    }
}
