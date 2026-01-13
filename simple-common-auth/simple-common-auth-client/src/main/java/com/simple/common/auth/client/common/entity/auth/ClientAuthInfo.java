package com.simple.common.auth.client.common.entity.auth;

import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA
 * 客户端权限信息
 *
 * @author qty
 */
public class ClientAuthInfo {

    //排除的url
    @Getter
    private final Map<String, Boolean> whiteMap = new ConcurrentHashMap<>();

    @Getter
    private final Map<String,HashSet<String>> ipMap = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, HashSet<String>> roleMap = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, HashSet<String>> byRoleMap = new ConcurrentHashMap<>();

    //是否是客户端
    private boolean client = false;

    //所有请求是否需要登录
    private boolean isLogin = false;

    //所有请求是否鉴权
    private boolean authentication = false;

    //是否只允许同时一人登陆
    private boolean oneLogin = false;

    //开启IP白名单
    @Getter
    private boolean iPWhitelist;

    //访问本项目需要的权限范围
    @Getter
    private final HashSet<String> scope = new HashSet<>();

    //当前所处环境
    @Getter
    private String produce;

    public UrlOperation antMatchers(String url) {
        return new UrlOperation(this, url);
    }

    public ClientAuthInfo anyClient() {
        client = true;
        return this;
    }

    public ClientAuthInfo openIPWhitelist() {
        iPWhitelist = true;
        return this;
    }

    public ClientAuthInfo openLogin() {
        isLogin = true;
        return this;
    }

    public ClientAuthInfo openAuthentication() {
        authentication = true;
        return this;
    }

    public ClientAuthInfo openOneLogin() {
        oneLogin = true;
        return this;
    }

    public void setDocument(String produce) {
        this.produce = produce;
    }

    public void setRoleMap(Map<String, HashSet<String>> map) {
        this.roleMap.clear();
        this.roleMap.putAll(map);
    }

    public void setByRoleMap(Map<String, HashSet<String>> map) {
        this.byRoleMap.clear();
        this.byRoleMap.putAll(map);
    }

    public void setWhiteMap(Map<String, Boolean> map) {
        this.whiteMap.clear();
        this.whiteMap.putAll(map);
    }

    public void setIpMap(Map<String, HashSet<String>> map) {
        this.ipMap.clear();
        this.ipMap.putAll(map);
    }

    public boolean getAuthentication() {
        return authentication;
    }

    public boolean getLogin() {
        return isLogin;
    }

    public boolean getOneLogin() {
        return oneLogin;
    }

    public boolean getClient() {
        return client;
    }

    public void addScope(String... scope) {
        this.scope.addAll(Arrays.asList(scope));
    }

}
