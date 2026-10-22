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
     * 契约：返回 true 表示「该路径配置了 IP 限制且请求 IP 在允许列表内」；
     * 返回 false 表示「路径未配置 IP 限制」或「配置了限制但 IP 不在允许列表」——
     * 两种情形本方法不抛异常，是否继续认证流程由调用方（拦截器）决定。
     * </p>
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 在认证拦截器中调用
     * if (whiteManager.checkWhiteIp(request.getRequestURI(), request.getRemoteAddr())) {
     *     // 在IP白名单中,直接放行
     *     chain.doFilter(request, response);
     * } else {
     *     // 不在IP白名单中,继续执行认证逻辑
     *     authenticate(request);
     * }
     * }</pre>
     *
     * @param path   当前请求的URL路径,如 /api/user/list
     * @param ipAddr 客户端IP地址,用于IP白名单校验
     * @return 是否在IP白名单内（true=命中IP规则且IP允许）
     */
    boolean checkWhiteIp(String path, String ipAddr);

}
