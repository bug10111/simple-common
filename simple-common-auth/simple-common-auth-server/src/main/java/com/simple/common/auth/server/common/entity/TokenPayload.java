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
     * 过期时间戳(秒)
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