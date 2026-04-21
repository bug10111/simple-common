package com.simple.common.auth.client.common.service;

/**
 * CSRF(Cross-Site Request Forgery)防御服务接口。
 * <p>
 * 提供CSRF Token的生成、保存、校验和删除功能,防止跨站请求伪造攻击。
 * 默认实现 {@link com.simple.common.auth.client.service.DefaultCsrfService} 基于Redis存储Token。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>表单提交保护：防止恶意网站伪造用户提交表单</li>
 *   <li>AJAX请求保护：为POST/PUT/DELETE等敏感操作添加CSRF校验</li>
 *   <li>会话安全：确保请求来自合法的客户端页面</li>
 * </ul>
 *
 * <h3>工作原理：</h3>
 * <ol>
 *   <li>服务器生成唯一的CSRF Token并保存到Session/Redis</li>
 *   <li>前端从服务器获取Token并在请求头或表单中携带</li>
 *   <li>服务器校验Token的有效性,校验通过则处理请求</li>
 *   <li>可选：校验后立即删除Token(一次性使用)</li>
 * </ol>
 *
 * <h3>扩展示例：</h3>
 * <pre>{@code
 * @Component
 * public class CustomCsrfService implements CsrfService {
 *     @Autowired
 *     private RedisTemplate<String, String> redisTemplate;
 *     
 *     @Override
 *     public void saveToken(String userId, String path, String token) {
 *         String key = "csrf:" + userId + ":" + path;
 *         redisTemplate.opsForValue().set(key, token, 30, TimeUnit.MINUTES);
 *     }
 *     
 *     @Override
 *     public void checkToken(String userId, String path, String token, boolean consume) {
 *         String key = "csrf:" + userId + ":" + path;
 *         String savedToken = redisTemplate.opsForValue().get(key);
 *         
 *         if (savedToken == null || !savedToken.equals(token)) {
 *             throw new SecurityException("CSRF Token无效");
 *         }
 *         
 *         // 如果consume为true,校验后删除Token
 *         if (consume) {
 *             redisTemplate.delete(key);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
public interface CsrfService {

    /**
     * 保存CSRF Token
     * <p>
     * 为指定用户和路径生成并保存CSRF Token。
     * Token通常有有效期限制,过期后需要重新获取。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 用户访问表单页面时生成Token
     * String token = UUID.randomUUID().toString();
     * csrfService.saveToken(userId, "/api/order/create", token);
     * 
     * // 将Token返回给前端
     * model.addAttribute("csrfToken", token);
     * }</pre>
     *
     * @param userId 用户ID,用于标识Token归属
     * @param path   请求路径,用于区分不同接口的Token
     * @param token  CSRF Token字符串,建议使用UUID或随机字符串
     */
    void saveToken(String userId, String path, String token);

    /**
     * 获取CSRF Token
     * <p>
     * 根据用户ID和路径获取已保存的CSRF Token。
     * 用于前端页面渲染时将Token嵌入表单或JavaScript变量。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // Controller层获取Token并传递给视图
     * @GetMapping("/order/form")
     * public String orderForm(Model model) {
     *     String userId = LoginUserUtils.getUserId();
     *     String token = csrfService.getToken(userId, "/api/order/create");
     *     model.addAttribute("csrfToken", token);
     *     return "order/form";
     * }
     * }</pre>
     *
     * @param userId 用户ID
     * @param path   请求路径
     * @return CSRF Token字符串,不存在返回null
     */
    String getToken(String userId, String path);

    /**
     * 删除CSRF Token
     * <p>
     * 移除指定用户和路径的CSRF Token。
     * 通常在用户登出或Token失效时调用。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 用户登出时清除所有CSRF Token
     * csrfService.removeToken(userId, "/api/order/create");
     * csrfService.removeToken(userId, "/api/user/update");
     * }</pre>
     *
     * @param userId 用户ID
     * @param path   请求路径
     */
    void removeToken(String userId, String path);

    /**
     * 校验CSRF Token
     * <p>
     * 验证请求中的CSRF Token是否有效。
     * 支持一次性Token模式(consume=true),校验通过后立即删除Token。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 在拦截器或过滤器中校验Token
     * @PostMapping("/order/create")
     * public R<Void> createOrder(@RequestBody OrderRequest request,
     *                            @RequestHeader("X-CSRF-TOKEN") String csrfToken) {
     *     String userId = LoginUserUtils.getUserId();
     *     
     *     // 校验Token,校验后删除(一次性使用)
     *     csrfService.checkToken(userId, "/api/order/create", csrfToken, true);
     *     
     *     // 处理业务逻辑
     *     orderService.create(request);
     *     return R.ok();
     * }
     * }</pre>
     *
     * @param userId  用户ID
     * @param path    请求路径
     * @param token   待校验的Token字符串
     * @param consume 是否在校验后立即删除Token
     *                - true: 一次性Token,校验通过后删除,防止重放攻击
     *                - false: 可重复使用,直到过期或被手动删除
     * @throws SecurityException 当Token无效、过期或不匹配时抛出异常
     */
    void checkToken(String userId, String path, String token, boolean consume);

}