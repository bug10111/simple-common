package com.simple.common.logs.client.common.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 日志服务接口。
 * <p>
 * 提供日志客户端的启动、停止和日志发送功能。
 * 默认实现 {@link com.simple.common.logs.client.service.DefaultLogService} 通过TCP协议
 * 将日志数据异步发送到日志服务器,支持高性能的日志收集和处理。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>操作日志记录：记录用户的增删改查操作</li>
 *   <li>访问日志记录：记录API接口的访问情况</li>
 *   <li>异常日志记录：记录系统异常和错误信息</li>
 *   <li>审计日志记录：用于安全审计和合规检查</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Autowired
 * private LogService logService;
 * 
 * // 在拦截器中自动调用(无需手动调用)
 * @AfterReturning
 * public void afterReturning(HttpServletRequest request, HttpServletResponse response, Object handler) {
 *     logService.send(request, response, handler, null);
 * }
 * 
 * @AfterThrowing
 * public void afterThrowing(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
 *     logService.send(request, response, handler, ex);
 * }
 * }</pre>
 *
 * @author qty
 */
public interface LogService {

    /**
     * 发送日志数据到日志服务器
     * <p>
     * 收集请求信息、响应信息、用户信息等,组装成日志事件并异步发送到日志服务器。
     * 该方法通常由AOP切面或拦截器自动调用,开发者无需手动调用。
     * </p>
     *
     * <h3>内部流程：</h3>
     * <ol>
     *   <li>从请求中提取URL、方法、参数等信息</li>
     *   <li>从响应中提取状态码、响应时间等信息</li>
     *   <li>从登录上下文获取操作用户信息</li>
     *   <li>组装日志事件对象</li>
     *   <li>通过TCP连接异步发送到日志服务器</li>
     * </ol>
     *
     * @param request  HTTP 请求对象
     * @param response HTTP 响应对象
     * @param handler  处理器对象(Controller方法)
     * @param ex       异常对象,如果请求处理过程中发生异常则不为null
     */
    void send(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex);

    /**
     * 启动日志客户端
     * <p>
     * 初始化TCP连接池,建立与日志服务器的通信通道。
     * 该方法通常在应用启动时由框架自动调用,无需手动调用。
     * </p>
     *
     * <h3>注意事项：</h3>
     * <ul>
     *   <li>确保日志服务器已启动并可访问</li>
     *   <li>配置正确的日志服务器地址和端口</li>
     *   <li>如果连接失败,会记录错误日志但不影响应用启动</li>
     * </ul>
     */
    void start();

    /**
     * 停止日志客户端
     * <p>
     * 关闭TCP连接池,释放相关资源。
     * 该方法通常在应用关闭时由框架自动调用,用于优雅停机。
     * </p>
     */
    void stop();


}