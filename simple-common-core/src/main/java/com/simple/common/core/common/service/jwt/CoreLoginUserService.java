package com.simple.common.core.common.service.jwt;

/**
 * 核心登录用户服务接口。
 * <p>
 * 提供获取当前登录用户基本信息的功能,如用户ID、用户名等。
 * 该接口通常由认证模块实现,用于在业务代码中快速获取当前用户身份。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>记录操作日志：获取当前操作用户ID</li>
 *   <li>数据权限过滤：根据用户ID过滤数据</li>
 *   <li>审计追踪：记录谁执行了某个操作</li>
 * </ul>
 *
 * <h3>扩展示例：</h3>
 * <pre>{@code
 * @Component
 * public class MyLoginUserService implements CoreLoginUserService {
 *     @Override
 *     public String getUserId() {
 *         // 从 SecurityContext 或 ThreadLocal 中获取用户ID
 *         Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 *         if (auth != null && auth.getPrincipal() instanceof UserDetails) {
 *             return ((UserDetails) auth.getPrincipal()).getUsername();
 *         }
 *         return null;
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
public interface CoreLoginUserService {

    /**
     * 获取当前登录用户ID
     * <p>
     * 从安全上下文或会话中获取当前登录用户的唯一标识。
     * 如果用户未登录,返回null或抛出异常(取决于实现)。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 在Service层获取当前用户ID
     * String userId = coreLoginUserService.getUserId();
     * 
     * // 创建订单时设置创建人
     * Order order = new Order();
     * order.setCreatorId(userId);
     * orderService.save(order);
     * }</pre>
     *
     * @return 当前登录用户的ID,未登录时可能返回null
     */
    String getUserId();

}
