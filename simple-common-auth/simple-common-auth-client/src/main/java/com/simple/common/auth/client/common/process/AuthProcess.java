package com.simple.common.auth.client.common.process;

import com.simple.common.core.common.service.process.BasProcessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 客户端鉴权处理接口。
 * <p>
 * 继承 {@link BasProcessService}，用于实现客户端请求的鉴权处理逻辑。
 * 通过责任链模式，多个鉴权处理器可以按顺序执行，实现Token校验、角色校验、权限校验等
 * 多种鉴权机制的联合处理。
 * </p>
 *
 * <h3>责任链配置：</h3>
 * <p>
 * 鉴权处理器的执行顺序通过 {@link com.simple.common.auth.client.common.enums.process.AuthInterceptorKindProcess}
 * 枚举定义，框架默认提供以下处理器（按执行顺序）：</p>
 * <ol>
 *   <li>{@link com.simple.common.auth.client.common.process.CheckTokenAuthProcess} - Token校验</li>
 *   <li>{@link com.simple.common.auth.client.common.process.CheckRoleAuthProcess} - 角色校验</li>
 *   <li>{@link com.simple.common.auth.client.common.process.CheckScopeAuthProcess} - 授权范围校验</li>
 * </ol>
 *
 * <h3>自定义扩展：</h3>
 * <p>
 * 如需添加自定义鉴权逻辑，可实现此接口并注册到 {@link com.simple.common.auth.client.common.enums.process.AuthInterceptorKindProcess} 枚举中。
 * </p>
 *
 * @author qty
 */
public interface AuthProcess extends BasProcessService {

    /**
     * 执行权限过滤。
     * <p>
     * 失败契约（实现方必须遵守，责任链按异常中断、按正常返回继续）：
     * 校验失败<b>必须抛出异常</b>（如 {@code LoginException}），禁止仅记日志后正常返回（等于放行）；
     * 禁止直接向 response 写入错误体（响应统一由全局异常处理器渲染）。
     * </p>
     * <p>
     * 参数契约：token 可能为 null（匿名请求已在前置判断中拦截，仍需防御）；
     * request/response/path 由框架保证非 null；ipAddr 可能因取源失败为 null。
     * </p>
     *
     * @param request  请求对象，非 null
     * @param response 响应对象，非 null（可用于设置响应头，不得写入错误体）
     * @param token    登录token，可能为 null，实现方自行判空
     * @param path     请求地址，非 null
     * @param ipAddr   IP地址，可能为 null
     */
    void execute(HttpServletRequest request, HttpServletResponse response, String token, String path, String ipAddr);
}