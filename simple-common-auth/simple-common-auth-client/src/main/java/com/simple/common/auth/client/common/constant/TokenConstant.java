package com.simple.common.auth.client.common.constant;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
public class TokenConstant {

    public static final String Authorization = "Authorization";

    //token前缀key
    public static final String bearerKey = "type";

    //token前缀
    public static final String bearer = "Bearer ";

    //client token前缀
    public static final String basic = "Basic ";

    //客户端id
    public static final String clientIdKey = "clientId";

    //客户端名称
    public static final String clientNameKey = "clientName";

    //微信appid
    public static final String wxAppIdKey = "wxAppId";

    //可访问的服务名称
    public static final String appNamesKey = "appNames";

    //授权范围
    public static final String scopesKey = "scopes";

    //登录角色
    public static final String loginRole = "loginRole";

    //token key
    public static final String accessTokenKey = "accessToken";

    //reToken key
    public static final String refreshTokenKey = "refreshToken";

    //登录信息
    public static final String loginInfoKey = "login:info";

    //权限信息
    public static final String userAuthKey = "login:auth";

    //用户权限关联
    public static final String userKey = "login:id";

    //登录用户id
    public static final String userIdKey = "userId";

    //登录页用户名称
    public static final String nicknameKey = "nickname";

    //登录用户id
    public static final String loginKey = "loginKey";

    //扩展信息
    public static final String extensionKey = "extension";

    //Token过期时间
    public static final String expKey = "exp";

    //接收者,这里用作存目标客户端
    public static final String audKey = "aud";

    //token有效时长
    public static final String etKey = "et";

    //AccessToken唯一标志
    public static final String jtiKey = "jti";

    //RefreshToken有效时长
    public static final String rEtKey = "ret";

    //RefreshToken唯一标志
    public static final String atiKey = "ati";

    /**
     * 获取内省登录的用户信息key
     *
     * @param key key
     */
    public static String getUserInfoKey(String key) {
        return TokenConstant.loginInfoKey + ":" + key;
    }

    /**
     * 获取登录用户的权限信息key
     *
     * @param key key
     */
    public static String getAuthKey(String key) {
        return TokenConstant.userAuthKey + ":" + key;
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
