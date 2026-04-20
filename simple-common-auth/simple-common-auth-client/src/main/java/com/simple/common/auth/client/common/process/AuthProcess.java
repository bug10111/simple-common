package com.simple.common.auth.client.common.process;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
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
     * 执行权限过滤器
     *
     * @param request  请求对象
     * @param response 响应对象 用于设置响应头
     * @param token    登录token
     * @param path     请求地址
     * @param ipAddr   IP地址
     */
    void execute(HttpServletRequest request, HttpServletResponse response, String token, String path, String ipAddr);
}