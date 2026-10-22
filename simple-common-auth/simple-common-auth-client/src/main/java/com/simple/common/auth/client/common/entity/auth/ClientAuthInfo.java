package com.simple.common.auth.client.common.entity.auth;

import com.simple.common.core.utils.AssertUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA
 * 客户端权限信息
 * <p>
 * 线程安全契约：配置方法（openLogin / openAuthentication / antMatchers / set*Map / addScope）仅在启动装配阶段（AuthInit 之前）调用；
 * 运行期仅允许读取。set*Map 采用「整体构建新 Map 后引用替换」的发布方式，运行期重配置（如权限热更新）
 * 对读者可见的是完整的新旧两态之一，不会出现清空/半更新的中间态。
 * </p>
 *
 * @author qty
 */
public class ClientAuthInfo {

    //排除的url
    private volatile Map<String, Boolean> whiteMap = new ConcurrentHashMap<>();

    private volatile Map<String, Set<String>> ipMap = new ConcurrentHashMap<>();

    private volatile Map<String, Set<String>> roleMap = new ConcurrentHashMap<>();

    private volatile Map<String, Set<String>> byRoleMap = new ConcurrentHashMap<>();

    //是否是客户端
    private volatile boolean client = false;

    //所有请求是否需要登录
    private volatile boolean isLogin = false;

    //所有请求是否鉴权
    private volatile boolean authentication = false;

    //开启IP白名单
    private volatile boolean ipWhitelist = false;

    //访问本项目需要的权限范围
    private final Set<String> scope = Collections.newSetFromMap(new ConcurrentHashMap<>());

    //当前所处环境
    private volatile String produce;

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
        this.client = true;
        return this;
    }

    /**
     * 开启IP白名单功能，启用后将对请求IP进行白名单校验
     *
     * @return 当前实例，支持链式调用
     */
    public ClientAuthInfo openIPWhitelist() {
        this.ipWhitelist = true;
        return this;
    }

    /**
     * 开启登录验证，所有请求都需要用户登录
     *
     * @return 当前实例，支持链式调用
     */
    public ClientAuthInfo openLogin() {
        this.isLogin = true;
        return this;
    }

    /**
     * 开启权限鉴权，所有请求都需要进行权限验证
     *
     * @return 当前实例，支持链式调用
     */
    public ClientAuthInfo openAuthentication() {
        this.authentication = true;
        return this;
    }

    /**
     * 设置当前应用所处的环境标识
     *
     * @param produce 环境标识，如 dev、test、produce 等
     */
    public void setProduce(String produce) {
        this.produce = produce;
    }

    /**
     * 设置当前应用所处的环境标识
     *
     * @param produce 环境标识
     * @deprecated 命名有歧义（易与 API 文档混淆），请使用 {@link #setProduce(String)}
     */
    @Deprecated
    public void setDocument(String produce) {
        setProduce(produce);
    }

    /**
     * 设置角色与URL的映射关系，用于基于角色的访问控制
     * <p>整体构建副本后原子替换引用，运行期更新不会暴露中间态。</p>
     *
     * @param map 角色映射表，key为URL路径，value为该URL允许访问的角色集合
     */
    public void setRoleMap(Map<String, Set<String>> map) {
        this.roleMap = copyOf(map);
    }

    /**
     * 设置反向角色映射关系，用于快速查询角色对应的URL权限
     * <p>整体构建副本后原子替换引用，运行期更新不会暴露中间态。</p>
     *
     * @param map 反向角色映射表，key为角色名，value为该角色可访问的URL集合
     */
    public void setByRoleMap(Map<String, Set<String>> map) {
        this.byRoleMap = copyOf(map);
    }

    /**
     * 设置白名单URL映射，这些URL不需要进行身份验证
     * <p>整体构建副本后原子替换引用。</p>
     *
     * @param map 白名单映射表，key为URL路径，value为是否排除的标识
     */
    public void setWhiteMap(Map<String, Boolean> map) {
        Objects.requireNonNull(map, "白名单映射不能为null");
        Map<String, Boolean> copy = new ConcurrentHashMap<>(map.size());
        copy.putAll(map);
        this.whiteMap = copy;
    }

    /**
     * 设置IP白名单映射，指定URL允许的IP地址集合
     * <p>整体构建副本后原子替换引用。</p>
     *
     * @param map IP白名单映射表，key为URL路径，value为允许的IP地址集合
     */
    public void setIpMap(Map<String, Set<String>> map) {
        this.ipMap = copyOf(map);
    }

    private static Map<String, Set<String>> copyOf(Map<String, Set<String>> map) {
        Objects.requireNonNull(map, "映射表不能为null");
        Map<String, Set<String>> copy = new ConcurrentHashMap<>(map.size());
        map.forEach((key, value) -> {
            Objects.requireNonNull(key, "映射键不能为null");
            // 值防御性拷贝为不可变集合，避免外部持引用绕过本类改写安全规则
            copy.put(key, value == null ? Set.of() : Set.copyOf(value));
        });
        return copy;
    }

    public Map<String, Boolean> getWhiteMap() {
        return whiteMap;
    }

    public Map<String, Set<String>> getIpMap() {
        return ipMap;
    }

    public Map<String, Set<String>> getRoleMap() {
        return roleMap;
    }

    public Map<String, Set<String>> getByRoleMap() {
        return byRoleMap;
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
     * 获取是否为客户端模式的标识
     *
     * @return true表示是客户端模式，false表示不是
     */
    public boolean getClient() {
        return client;
    }

    /**
     * 获取是否开启IP白名单
     * <p>保留旧 getter 名以兼容既有调用方（历史字段名 iPWhitelist 经 Lombok 生成的名称）。</p>
     *
     * @return true表示开启IP白名单校验
     */
    public boolean isIPWhitelist() {
        return ipWhitelist;
    }

    /**
     * 获取当前所处环境标识
     *
     * @return 环境标识（如 produce），可能为 null（未配置 spring.profiles.active）
     */
    public String getProduce() {
        return produce;
    }

    /**
     * 获取访问本项目需要的权限范围（只读视图）
     *
     * @return 权限范围集合（不可修改视图）
     */
    public Set<String> getScope() {
        return Collections.unmodifiableSet(scope);
    }

    /**
     * 添加权限范围标识，用于定义当前应用需要的OAuth2权限范围
     *
     * @param scope 权限范围字符串数组，如 "read", "write" 等；null 数组或 null 元素会被显式拒绝
     */
    public void addScope(String... scope) {
        Objects.requireNonNull(scope, "权限范围不能为null");
        for (String s : scope) {
            AssertUtils.notEmpty(s, "权限范围元素不能为空");
            this.scope.add(s);
        }
    }

}
