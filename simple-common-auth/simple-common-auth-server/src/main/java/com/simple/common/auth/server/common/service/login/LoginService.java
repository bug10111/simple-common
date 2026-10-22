package com.simple.common.auth.server.common.service.login;

import com.simple.common.auth.server.common.adapter.LoginTypeAdapter;

import java.util.Map;

/**
 * 登录服务接口。
 * <p>
 * 提供统一的登录、登出、token刷新等核心认证功能。
 * 默认实现 {@link com.simple.common.auth.server.service.login.DefaultLoginService} 根据登录类型
 * 自动路由到对应的 {@link com.simple.common.auth.server.common.manager.login.LoginManager} 实现。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Autowired
 * private LoginService loginService;
 *
 * public TokenData login(String username, String password) {
 *     PwdLoginRequest request = new PwdLoginRequest(username, password);
 *     return loginService.login(request, LoginTypeAdapter.PASSWORD);
 * }</pre>
 *
 * @author qty
 */
public interface LoginService {

    /**
     * 用户登录
     *
     * @param adapter   登录请求对象(如 PwdLoginRequest、SmsLoginRequest 等)
     * @param loginType 登录类型,用于路由到对应的 LoginManager
     * @return Token 数据Map，包含 type（Bearer）、accessToken、refreshToken、exp、scopes 等键（键名见 TokenConstant）
     * @throws com.simple.common.core.exception.DefaultException 登录失败时抛出（业务码由 LoginException 等枚举承载，如密码错误、账号锁定等）
     */
    Map<String, String> login(Object adapter, LoginTypeAdapter loginType);

    /**
     * 刷新 Access Token
     *
     * @param refreshTokenStr 刷新 Token 字符串
     * @return 新的 Token 数据Map，键同 {@link #login(Object, LoginTypeAdapter)}
     * @throws com.simple.common.core.exception.DefaultException Refresh Token 无效/过期/已被消费（重放）时抛出
     */
    Map<String, String> refresh(String refreshTokenStr);

    /**
     * 退出登录(指定用户)
     *
     * @param userId 用户ID
     * @throws RuntimeException 当用户不存在时抛出异常
     */
    void logout(String userId);

    /**
     * 退出登录(当前用户)
     * <p>
     * 从当前请求上下文中获取用户ID并退出登录
     * </p>
     *
     * @throws RuntimeException 当未检测到登录用户时抛出异常
     */
    void logout();

}