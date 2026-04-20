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
     * 登录
     *
     * @param adapter   请求对象实体
     * @param loginType 登录类型
     */
    Map<String, String> login(Object adapter, LoginTypeAdapter loginType);

    /**
     * 刷新登录
     *
     * @param refreshTokenStr 刷新token
     */
    Map<String, String> refresh(String refreshTokenStr);

    /**
     * 退出登录
     *
     * @param userId 用户id
     */
    void logout(String userId);

    /**
     * 退出登录
     */
    void logout();

}