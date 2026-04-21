package com.simple.common.auth.server.common.service.login;

import com.simple.common.auth.server.common.adapter.LoginTypeAdapter;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.entity.TokenData;

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
 *     ClientDetails client = clientDetailsService.getClientDetails(header);
 *     return loginService.login(request, LoginTypeAdapter.PASSWORD, client);
 * }
 * }</pre>
 *
 * @author qty
 */
public interface LoginService {

    /**
     * 用户登录
     *
     * @param adapter   登录请求对象(如 PwdLoginRequest、SmsLoginRequest 等)
     * @param loginType 登录类型枚举,用于路由到对应的 LoginManager
     * @return Token 数据Map,包含 accessToken、refreshToken、expiresIn 等信息
     * @throws RuntimeException 当登录失败时抛出异常(如密码错误、账号锁定等)
     */
    Map<String, String> login(Object adapter, LoginTypeAdapter loginType);

    /**
     * 刷新 Access Token
     *
     * @param refreshTokenStr 刷新 Token 字符串
     * @return 新的 Token 数据Map,包含新的 accessToken、refreshToken、expiresIn 等
     * @throws RuntimeException 当 Refresh Token 无效或已过期时抛出异常
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