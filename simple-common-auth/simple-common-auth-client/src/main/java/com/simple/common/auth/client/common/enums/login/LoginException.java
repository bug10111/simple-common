package com.simple.common.auth.client.common.enums.login;

import com.simple.common.core.exception.AbstractException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 * 登录相关业务异常。
 * <p>
 * 错误码语义（客户端按码分型处理，不依赖 message）：
 * <ul>
 *   <li>1000 LOGIN_EXPIRED：登录态过期/令牌非法，access token 失效——客户端应走刷新链路；</li>
 *   <li>1001 RE_LOGIN_EXPIRED：刷新令牌失效或验签失败/密钥缺失——客户端应重新登录；</li>
 *   <li>1002 INSUFFICIENT_PERMISSIONS：已登录但权限不足——提示联系管理员；</li>
 *   <li>1003 LOGIN_ERROR_NUM：同一账号单位时间登录失败次数超限——提示稍后再试；</li>
 *   <li>1004 LOGIN_IP_ERROR_NUM：同一IP单位时间登录失败次数超限——提示稍后再试。</li>
 * </ul>
 * 1000~1999 为 auth 模块登录域号段；跨模块（如 websocket 1001/1002/1003）存在码段重叠，
 * 网关/客户端须结合服务来源解析，禁止跨模块按裸码判型（历史遗留契约，暂不变更码值）。
 * </p>
 *
 * @author qty
 */
@Getter
@AllArgsConstructor
public enum LoginException implements AbstractException {

    /**
     * 登录态过期（access token 失效，可刷新）
     */
    LOGIN_EXPIRED("1000", "登录失效，请重新登录"),

    /**
     * 需要重新登录（刷新令牌失效/验签失败，不可刷新）
     */
    RE_LOGIN_EXPIRED("1001", "登录失效，请重新登录"),

    /**
     * 权限不足
     */
    INSUFFICIENT_PERMISSIONS("1002", "权限不足，请联系管理员"),

    /**
     * 登录失败次数过多（账号维度）
     */
    LOGIN_ERROR_NUM("1003", "登录失败次数过多，请稍后再试"),

    /**
     * 登录失败次数过多（IP维度）
     */
    LOGIN_IP_ERROR_NUM("1004", "该IP登录失败次数过多，请稍后再试"),

    ;

    private final String code;

    private final String message;

}
