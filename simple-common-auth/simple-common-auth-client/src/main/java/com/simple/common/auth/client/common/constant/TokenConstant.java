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
     * HTTP 授权头前缀解析专用，前缀含空格用于区分头值中的 token 部分
     */
    public static final String bearer = "Bearer ";

    /**
     * Token类型契约值
     * 登录与刷新响应 type 字段的标准契约值，RFC 6750 规定 token_type 为 "Bearer"（不含空格）；
     * 与授权头前缀常量 bearer 职责不同，本常量用于响应体字段值
     */
    public static final String bearerValue = "Bearer";

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
     * 身份归属域 key（登录主体认证域的结构化标识；平台域令牌固定值=platform）
     */
    public static final String domainKey = "domain";

    /**
     * 数据权限 key
     */
    public static final String dataPermissionKey = "dataPermission";

    /**
     * 行事域标识 key（代管行事上下文三要素之一，delegation 签发时置位；与 TokenPayload.actingDomain 字段名逐字一致）
     * <p>载荷该键非空=代管行事令牌（D2 同通道传递）：业务端校验链按行事分支放行
     * （吊销态判定+载荷自包含构造用户上下文），不走认证域会话内省（行事令牌无 login:info 会话）。</p>
     */
    public static final String actingDomainKey = "actingDomain";

    /**
     * 行事企业标识 key（代管行事上下文三要素之二；与数据权限槽位 dataPermission.tenantId 同值）
     */
    public static final String actingTenantIdKey = "actingTenantId";

    /**
     * 审计归属标记载荷键（代管行事上下文；值形态 platform:{平台员工标识}）
     */
    public static final String auditMarkerKey = "auditMarker";

    /**
     * 平台超管显式标识载荷键（与 TokenPayload.superAdmin 字段名一致；行事令牌自载荷透传）
     */
    public static final String superAdminKey = "superAdmin";

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
     * @param key jti（Token 唯一标识），不能为空
     */
    public static String getUserInfoKey(String key) {
        AssertUtils.notEmpty(key, "获取用户信息缓存key失败：jti不能为空");
        return TokenConstant.loginInfoKey + ":" + key;
    }

    /**
     * 获取登录用户的权限信息key（按项目+租户维度）
     * <p>
     * 支持项目+租户维度的权限隔离，缓存 key 格式为：login:auth:{roleKey}:{projectCode}:{tenantId}；
     * tenantId 为空时保持旧格式（无租户段），兼容无租户归属的历史角色。
     * 一个角色可能属于多个项目模块，因此 roleKey 在前便于按角色查询；
     * 同项目跨租户同名 roleKey 的权限互不覆盖（RLS 多租户形态的隔离基础）。
     * </p>
     *
     * @param roleKey     角色标识，如 "admin"、"editor"
     * @param projectCode 项目编码，如 "xiaoyue"、"erp_system"
     * @param tenantId    租户 ID（角色归属租户；无租户归属的历史角色传空）
     * @return 权限缓存 key
     */
    public static String getAuthKey(String roleKey, String projectCode, String tenantId) {
        AssertUtils.notEmpty(roleKey, "角色标识不能为空");
        AssertUtils.notEmpty(projectCode, "项目编码不能为空");
        // 键分隔符为 ':'，各维度取值含 ':' 会造成键歧义（跨租户/跨项目权限别名），直接拒绝
        AssertUtils.isTrue(roleKey.indexOf(':') < 0, "角色标识不能包含分隔符':'");
        AssertUtils.isTrue(projectCode.indexOf(':') < 0, "项目编码不能包含分隔符':'");
        AssertUtils.isTrue(tenantId == null || tenantId.indexOf(':') < 0, "租户ID不能包含分隔符':'");
        String base = TokenConstant.userAuthKey + ":" + roleKey + ":" + projectCode;
        return (tenantId == null || tenantId.isEmpty()) ? base : base + ":" + tenantId;
    }

    /**
     * 获取登录用户的权限信息key（按项目维度·无租户角色兼容入口）
     *
     * @param roleKey     角色标识，如 "admin"、"editor"
     * @param projectCode 项目编码，如 "xiaoyue"、"erp_system"
     * @return 权限缓存 key
     */
    public static String getAuthKey(String roleKey, String projectCode) {
        return getAuthKey(roleKey, projectCode, null);
    }

    /**
     * 获取登录用户和权限关联的key
     *
     * @param userId 用户ID，不能为空（为空会退化为共享键 login:id:null，造成跨用户令牌集合串写）
     */
    public static String getUserTokenKey(String userId) {
        AssertUtils.notEmpty(userId, "获取用户Token关联key失败：用户ID不能为空");
        return TokenConstant.userKey + ":" + userId;
    }

}