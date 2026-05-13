package com.simple.common.auth.client.common.constant;

import com.simple.common.core.utils.AssertUtils;

/**
 * Token相关常量定义
 * 包含JWT字段key、缓存key、请求头名称等常量
 *
 * @author qty
 */
public class TokenConstant {

    /**
     * 用户上下文内部传递头
     */
    public static final String userHead = "X-User-Context";

    /**
     * 用户签名头
     */
    public static final String userSignHead = "X-User-Signature";

    /**
     * HTTP授权头
     */
    public static final String Authorization = "Authorization";

    /**
     * Token类型前缀key
     */
    public static final String bearerKey = "type";

    /**
     * Bearer Token前缀
     */
    public static final String bearer = "Bearer ";

    /**
     * Basic Auth前缀
     */
    public static final String basic = "Basic ";

    /**
     * 客户端ID key
     */
    public static final String clientIdKey = "clientId";

    /**
     * 客户端名称 key
     */
    public static final String clientNameKey = "clientName";

    /**
     * 微信小程序AppID key
     */
    public static final String wxAppIdKey = "wxAppId";

    /**
     * 可访问的服务名称 key
     */
    public static final String appNamesKey = "appNames";

    /**
     * 授权范围 key
     */
    public static final String scopesKey = "scopes";

    /**
     * 登录角色 key
     */
    public static final String loginRole = "loginRole";

    /**
     * Access Token key
     */
    public static final String accessTokenKey = "accessToken";

    /**
     * Refresh Token key
     */
    public static final String refreshTokenKey = "refreshToken";

    /**
     * 登录信息缓存前缀
     */
    public static final String loginInfoKey = "login:info";

    /**
     * 用户权限缓存前缀
     */
    public static final String userAuthKey = "login:auth";

    /**
     * 用户Token关联缓存前缀
     */
    public static final String userKey = "login:id";

    /**
     * 用户ID key
     */
    public static final String userIdKey = "userId";

    /**
     * 用户昵称 key
     */
    public static final String nicknameKey = "nickname";

    /**
     * 登录标识 key
     */
    public static final String loginKey = "loginKey";

    /**
     * 扩展信息 key
     */
    public static final String extensionKey = "extension";

    /**
     * 数据权限 key
     */
    public static final String dataPermissionKey = "dataPermission";

    /**
     * Token过期时间 key
     */
    public static final String expKey = "exp";

    /**
     * 受众(Audience) key，用于存储目标客户端
     */
    public static final String audKey = "aud";

    /**
     * Access Token有效时长 key
     */
    public static final String etKey = "et";

    /**
     * Access Token唯一标识 key
     */
    public static final String jtiKey = "jti";

    /**
     * Refresh Token有效时长 key
     */
    public static final String rEtKey = "ret";

    /**
     * Refresh Token唯一标识 key
     */
    public static final String atiKey = "ati";

    /**
     * 用户详情（远程同步用）
     */
    public static final String userInfoName = "userInfo";

    /**
     * 用户权限（远程同步用）
     */
    public static final String userAuthName = "userAuth";

    /**
     * 用户权限关联（远程同步用）
     */
    public static final String userTokenName = "userToken";

    /**
     * 获取内省登录的用户信息key
     *
     * @param key key
     */
    public static String getUserInfoKey(String key) {
        return TokenConstant.loginInfoKey + ":" + key;
    }

    /**
     * 获取登录用户的权限信息key（按项目维度）
     * <p>
     * 支持项目维度的权限隔离，缓存 key 格式为：login:auth:{roleKey}:{projectCode}
     * 一个角色可能属于多个项目模块，因此 roleKey 在前便于按角色查询。
     * </p>
     *
     * @param roleKey     角色标识，如 "admin"、"editor"
     * @param projectCode 项目编码，如 "xiaoyue"、"erp_system"
     * @return 权限缓存 key
     */
    public static String getAuthKey(String roleKey, String projectCode) {
        AssertUtils.notEmpty(roleKey, "角色标识不能为空");
        AssertUtils.notEmpty(projectCode, "项目编码不能为空");
        return TokenConstant.userAuthKey + ":" + roleKey + ":" + projectCode;
    }

    /**
     * 获取登录用户和权限关联的key
     *
     * @param userId key
     */
    public static String getUserTokenKey(String userId) {
        return TokenConstant.userKey + ":" + userId;
    }

}