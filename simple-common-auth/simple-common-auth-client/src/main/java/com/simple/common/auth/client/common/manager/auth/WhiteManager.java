package com.simple.common.auth.client.common.manager.auth;

/**
 * 白名单管理器接口。
 * <p>
 * 用于检查请求路径和IP是否在白名单中,白名单请求无需进行认证和权限校验。
 * 默认实现 {@link com.simple.common.auth.client.manager.auth.DefaultWhiteManager} 不做任何校验,
 * 所有请求都会通过认证流程。
 * </p>
 *
 * <h3>扩展方式：</h3>
 * <p>
 * 如需自定义白名单逻辑(如从数据库加载、动态配置等),请继承 
 * {@link com.simple.common.auth.client.manager.auth.DefaultWhiteManager} 并重写相关方法。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>公开接口无需登录即可访问(如登录接口、注册接口)</li>
 *   <li>健康检查接口(/actuator/health)</li>
 *   <li>静态资源访问(/static/**)</li>
 *   <li>特定IP地址的直接访问(如内网服务调用)</li>
 * </ul>
 *
 * @author qty
 */
public interface WhiteManager {

    /**
     * 校验请求是否在白名单中
     * <p>
     * 检查请求路径和IP地址是否在白名单配置中。
     * 如果在白名单中,则直接放行,不进行后续的认证和权限校验。
     * 如果不在白名单中,则抛出异常或继续执行认证流程。
     * </p>
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 在认证拦截器中调用
     * try {
     *     whiteManager.checkWhiteIp(request.getPath(), request.getRemoteAddr());
     *     // 在白名单中,直接放行
     *     chain.doFilter(request, response);
     * } catch (WhiteListException e) {
     *     // 不在白名单中,继续执行认证逻辑
     *     authenticate(request);
     * }
     * }</pre>
     *
     * @param path   当前请求的URL路径,如 /api/user/list
     * @param ipAddr 客户端IP地址,用于IP白名单校验
     * @return 是否通过校验
     * @throws RuntimeException 当请求不在白名单中且需要拦截时抛出(可选)
     */
    boolean checkWhiteIp(String path, String ipAddr);

}
