package com.simple.common.auth.server.common.manager.login;

import com.simple.common.auth.server.common.entity.AbsUserDetails;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.entity.TokenData;

/**
 * 登录管理器接口。
 * <p>
 * 用于处理特定登录类型的用户认证逻辑。
 * 不同登录方式(如密码登录、微信登录、短信登录等)通过实现此接口来定义各自的认证流程。
 * 默认抽象实现 {@link AbsLoginManager} 提供了通用的登录流程框架,包括参数校验、用户查询、Token生成等。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>密码登录：验证用户名和密码</li>
 *   <li>短信登录：验证手机号和验证码</li>
 *   <li>微信登录：验证微信授权码</li>
 *   <li>第三方OAuth登录：验证第三方Token</li>
 * </ul>
 *
 * <h3>扩展示例：</h3>
 * <pre>{@code
 * @Component
 * public class PwdLoginManager extends AbsLoginManager<PwdLoginRequest> {
 *     @Override
 *     protected AbsUserDetails doLogin(PwdLoginRequest request, ClientDetails clientDetails) {
 *         // 1. 验证账号是否存在
 *         SysUser user = userService.findByUsername(request.getUsername());
 *         AssertUtils.isTrue(user != null, LoginException.USER_NOT_FOUND);
 *         
 *         // 2. 验证密码是否正确
 *         AssertUtils.isTrue(passwordEncoder.matches(request.getPassword(), user.getPassword()), 
 *                           LoginException.PASSWORD_ERROR);
 *         
 *         // 3. 检查账号状态
 *         AssertUtils.isTrue(user.isEnabled(), LoginException.ACCOUNT_DISABLED);
 *         
 *         return user;
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
public interface LoginManager {

    /**
     * 判断是否支持该登录请求类型
     * <p>
     * 根据请求参数的类型或内容,判断当前 LoginManager 是否能处理该登录请求。
     * 框架会根据此方法的返回值自动路由到对应的 LoginManager 实现。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * @Override
     * public boolean support(Object adapter) {
     *     return adapter instanceof PwdLoginRequest;
     * }
     * }</pre>
     *
     * @param adapter 登录请求对象(如 PwdLoginRequest、SmsLoginRequest 等)
     * @return true 表示支持该类型的登录请求,false 表示不支持
     */
    boolean support(Object adapter);

    /**
     * 执行登录并获取用户信息
     * <p>
     * 根据客户端信息和登录参数进行认证,认证成功后返回用户详细信息。
     * 该方法由框架调用,开发者只需关注业务认证逻辑。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * @Override
     * public AbsUserDetails login(ClientDetails clientDetails, Object adapter) {
     *     PwdLoginRequest request = (PwdLoginRequest) adapter;
     *     
     *     // 验证账号密码
     *     SysUser user = userService.findByUsername(request.getUsername());
     *     AssertUtils.isTrue(user != null, LoginException.USER_NOT_FOUND);
     *     AssertUtils.isTrue(passwordEncoder.matches(request.getPassword(), user.getPassword()), 
     *                       LoginException.PASSWORD_ERROR);
     *     
     *     return user;
     * }
     * }</pre>
     *
     * @param clientDetails 客户端详情信息,包含 clientId、scopes 等配置
     * @param adapter       登录参数对象,需转换为具体的登录请求类型
     * @return 认证通过的用户详细信息,包含 userId、username、roles、permissions 等
     * @throws RuntimeException 当认证失败时抛出异常(如密码错误、账号锁定等)
     */
    AbsUserDetails login(ClientDetails clientDetails, Object adapter);
}