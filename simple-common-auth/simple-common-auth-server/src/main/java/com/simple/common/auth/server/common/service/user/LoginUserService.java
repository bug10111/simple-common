package com.simple.common.auth.server.common.service.user;

import com.simple.common.auth.server.common.entity.AbsUserDetails;

import java.util.Map;

/**
 * 登录用户服务接口。
 * <p>
 * 提供用户详情获取功能,用于Token刷新和用户信息查询。
 * 该接口由认证服务端实现,负责从数据库或其他数据源获取用户详细信息。
 * 默认实现 {@link com.simple.common.auth.server.service.user.DefaultLoginUserService}
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>Token刷新：获取最新用户信息更新到Token中</li>
 *   <li>用户查询：根据用户ID获取完整的用户资料</li>
 *   <li>权限加载：获取用户的角色和权限信息</li>
 * </ul>
 *
 * <h3>扩展示例：</h3>
 * <pre>{@code
 * @Service
 * public class CustomLoginUserService implements LoginUserService {
 *     @Autowired
 *     private UserRepository userRepository;
 *     
 *     @Override
 *     public Map<String, String> getUserInformation() {
 *         // 从安全上下文获取当前用户ID
 *         String userId = SecurityUtils.getCurrentUserId();
 *         
 *         // 查询用户详细信息
 *         User user = userRepository.findById(userId);
 *         
 *         // 构建用户信息Map
 *         Map<String, String> userInfo = new HashMap<>();
 *         userInfo.put("userId", user.getId());
 *         userInfo.put("username", user.getUsername());
 *         userInfo.put("nickName", user.getNickName());
 *         userInfo.put("email", user.getEmail());
 *         userInfo.put("phone", user.getPhone());
 *         
 *         return userInfo;
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
public interface LoginUserService {

    /**
     * 获取当前登录用户内省信息
     * <p>
     * 获取当前登录用户的详细信息,通常用于Token刷新时更新用户数据。
     * 该方法应该从安全上下文或会话中获取当前用户ID,然后查询完整的用户信息。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 在Token刷新时使用
     * @PostMapping("/refresh")
     * public R<TokenResponse> refreshToken(@RequestBody RefreshRequest request) {
     *     // 验证Refresh Token
     *     Map<String, Object> payload = tokenManager.check(request.getRefreshToken(), true);
     *     
     *     // 获取最新的用户信息
     *     Map<String, String> userInfo = loginUserService.getUserInformation();
     *     
     *     // 生成新的Access Token
     *     String newAccessToken = tokenManager.create(userInfo);
     *     
     *     return R.ok(new TokenResponse(newAccessToken));
     * }
     * }</pre>
     *
     * @return 用户详细信息Map,包含userId、username、nickName等字段
     * @throws RuntimeException 当用户不存在或查询失败时抛出异常
     */
    Map<String, String> getUserInformation();

}