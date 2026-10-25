package com.simple.common.auth.server.common.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Set;

/**
 * Token载荷数据实体
 * 用于统一管理JWT令牌中的用户信息和客户端信息
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
public class TokenPayload {

    /**
     * JWT唯一标识
     */
    private String jti;

    /**
     * Access Token唯一标识
     */
    private String ati;

    /**
     * 过期时间戳(毫秒)：与 TokenPayload.create/refresh 的写入及 TokenManager 校验口径一致
     */
    private Long exp;

    /**
     * 受众(Audience)，标识令牌的接收者
     */
    private String aud;

    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 客户端名称
     */
    private String clientName;

    /**
     * 微信小程序AppID
     */
    private String wxAppId;

    /**
     * 应用名称列表(逗号分隔)
     */
    private String appNames;

    /**
     * 权限范围列表
     */
    private List<String> scopes;

    /**
     * 登录角色集合
     */
    private Set<String> loginRole;

    /**
     * 平台超级管理员显式标识（授权中心登录时置位；null=未置位，载荷落缓存时按 null 属性剔除）
     */
    private Boolean superAdmin;

    /**
     * 身份归属域（登录主体认证域的结构化标识）
     * <p>授权中心平台控制台登录（N12）时置位，固定值=platform（平台域令牌）；
     * 租户域登录不置位，null=租户域令牌，载荷落缓存时按 null 属性剔除
     * （superAdmin/actingDomain 显式标识先例同款扩展点）。
     * 与行事域标识 actingDomain 语义分工：domain=登录主体归属域（谁在认证），
     * actingDomain=代管行事域（是否在代管行事），两者独立置位互不推导。</p>
     */
    private String domain;

    /**
     * 平台岗位集（平台令牌岗位授权的权威口径载体）
     * <p>平台登录时承载 platform 岗位绑定集（X1 契约出参岗位段），
     * 供代管令牌签发准入校验（选定岗位∈平台令牌岗位集）与平台域消费方读取；
     * 租户域登录不置位，null=非平台令牌，载荷落缓存时按 null 属性剔除。</p>
     */
    private List<String> platformRoleIds;

    /**
     * 行事域标识（代管行事上下文三要素之一，授权中心代管签发时置位）
     * <p>固定值=平台域，以载荷结构字段结构化表达（非租户码/角色键判断）；
     * null=非代管令牌，载荷落缓存时按 null 属性剔除（superAdmin 显式标识先例同款扩展点）。</p>
     */
    private String actingDomain;

    /**
     * 行事企业标识（代管行事上下文三要素之二）
     * <p>行事期间的企业；与数据权限槽位的企业标识（dataPermission.tenantId）同值，
     * RLS 通道消费点零改动；null=非代管令牌，载荷落缓存时按 null 属性剔除。</p>
     */
    private String actingTenantId;

    /**
     * 行事岗位集（代管行事上下文三要素之三）
     * <p>行事期间授权岗位集合，签发时单岗位、按集合形态承载以兼容升格；
     * null=非代管令牌，载荷落缓存时按 null 属性剔除。</p>
     */
    private List<String> actingRoleIds;

    /**
     * 审计归属标记（代管行事上下文）
     * <p>值形态 platform:{平台员工标识}；行事上下文读取出参、审计归属标记旁路入参、
     * 存量表审计列值形态三处同源；null=非代管令牌，载荷落缓存时按 null 属性剔除。</p>
     */
    private String auditMarker;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 登录标识(用于区分同一用户不同登录设备)
     */
    private String loginKey;

    /**
     * 刷新令牌有效期(秒)
     */
    private Integer ret;

    /**
     * 访问令牌有效期(秒)
     */
    private Integer et;

    /**
     * 扩展信息(JSON格式)
     */
    private String extension;

    /**
     * 数据权限(JSON格式)
     */
    private String dataPermission;
}