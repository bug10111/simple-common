package com.simple.common.auth.client.common.entity.auth;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
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

    /**
     * 创建URL匹配操作器，用于配置特定URL的访问规则
     *
     * @param url URL路径，支持Ant风格的路径匹配（如 /api/**）
     * @return URL操作器实例，用于链式配置
     */
    public UrlOperation antMatchers(String url) {
        return new UrlOperation(this, url);
    }

    /**
     * 标记当前应用为客户端模式，允许作为OAuth2客户端进行认证
     *
     * @return 当前实例，支持链式调用
     */
    public ClientAuthInfo anyClient() {
        client = true;
        return this;
    }

    /**
     * 开启IP白名单功能，启用后将对请求IP进行白名单校验
     *
     * @return 当前实例，支持链式调用
     */
    public ClientAuthInfo openIPWhitelist() {
        iPWhitelist = true;
        return this;
    }

    /**
     * 开启登录验证，所有请求都需要用户登录
     *
     * @return 当前实例，支持链式调用
     */
    public ClientAuthInfo openLogin() {
        isLogin = true;
        return this;
    }

    /**
     * 开启权限鉴权，所有请求都需要进行权限验证
     *
     * @return 当前实例，支持链式调用
     */
    public ClientAuthInfo openAuthentication() {
        authentication = true;
        return this;
    }

    /**
     * 开启单点登录限制，同一账号只允许同时一人在线
     *
     * @return 当前实例，支持链式调用
     */
    public ClientAuthInfo openOneLogin() {
        oneLogin = true;
        return this;
    }

    /**
     * 设置当前应用所处的环境标识
     *
     * @param produce 环境标识，如 dev、test、produce 等
     */
    public void setDocument(String produce) {
        this.produce = produce;
    }

    /**
     * 设置角色与URL的映射关系，用于基于角色的访问控制
     *
     * @param map 角色映射表，key为URL路径，value为该URL允许访问的角色集合
     */
    public void setRoleMap(Map<String, HashSet<String>> map) {
        this.roleMap.clear();
        this.roleMap.putAll(map);
    }

    /**
     * 设置反向角色映射关系，用于快速查询角色对应的URL权限
     *
     * @param map 反向角色映射表，key为角色名，value为该角色可访问的URL集合
     */
    public void setByRoleMap(Map<String, HashSet<String>> map) {
        this.byRoleMap.clear();
        this.byRoleMap.putAll(map);
    }

    /**
     * 设置白名单URL映射，这些URL不需要进行身份验证
     *
     * @param map 白名单映射表，key为URL路径，value为是否排除的标识
     */
    public void setWhiteMap(Map<String, Boolean> map) {
        this.whiteMap.clear();
        this.whiteMap.putAll(map);
    }

    /**
     * 设置IP白名单映射，指定URL允许的IP地址集合
     *
     * @param map IP白名单映射表，key为URL路径，value为允许的IP地址集合
     */
    public void setIpMap(Map<String, HashSet<String>> map) {
        this.ipMap.clear();
        this.ipMap.putAll(map);
    }

    /**
     * 获取是否开启权限鉴权的标识
     *
     * @return true表示开启鉴权，false表示不开启
     */
    public boolean getAuthentication() {
        return authentication;
    }

    /**
     * 获取是否开启登录验证的标识
     *
     * @return true表示需要登录，false表示不需要
     */
    public boolean getLogin() {
        return isLogin;
    }

    /**
     * 获取是否开启单点登录限制的标识
     *
     * @return true表示限制单人登录，false表示不限制
     */
    public boolean getOneLogin() {
        return oneLogin;
    }

    /**
     * 获取是否为客户端模式的标识
     *
     * @return true表示是客户端模式，false表示不是
     */
    public boolean getClient() {
        return client;
    }

    /**
     * 添加权限范围标识，用于定义当前应用需要的OAuth2权限范围
     *
     * @param scope 权限范围字符串数组，如 "read", "write" 等
     */
    public void addScope(String... scope) {
        this.scope.addAll(Arrays.asList(scope));
    }

}
