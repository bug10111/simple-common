package com.simple.common.auth.server.common.process;

import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.core.common.service.process.BasProcessService;

/**
 * 登录错误处理接口。
 * <p>
 * 继承 {@link BasProcessService}，用于实现登录失败时的错误处理逻辑。
 * 通过责任链模式，多个错误处理器可以按顺序执行，实现账号锁定、IP限制等
 * 多种错误处理机制的联合处理。
 * </p>
 *
 * <h3>责任链配置：</h3>
 * <p>
 * 错误处理器的执行顺序通过 {@link com.simple.common.auth.server.common.enums.process.LoginErrorKindProcess}
 * 枚举定义，框架默认提供以下处理器（按执行顺序）：</p>
 * <ol>
 *   <li>{@link com.simple.common.auth.server.process.IpLoginErrorProcess} - IP错误次数限制</li>
 * </ol>
 *
 * <h3>自定义扩展：</h3>
 * <p>
 * 如需添加自定义错误处理逻辑（如短信验证码错误限制），可实现此接口并注册到
 * {@link com.simple.common.auth.server.common.enums.process.LoginErrorKindProcess} 枚举中。抽象基类 {@link com.simple.common.auth.server.common.process.AbsLoginErrorProcess}
 * 提供了Redis缓存和配置属性注入，可继承简化实现。
 * </p>
 *
 * @author qty
 */
public interface LoginErrorProcess extends BasProcessService {

    /**
     * 检查登录失败次数是否超限
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     * @param ip            请求IP
     * @return true-通过校验，false-不通过
     */
    boolean checkErrorNum(ClientDetails clientDetails, Object adapter, String ip);

    /**
     * 记录登录失败
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     * @param ip            请求IP
     */
    void recordError(ClientDetails clientDetails, Object adapter, String ip);

    /**
     * 清除登录失败记录（登录成功后调用）
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     * @param ip            请求IP
     */
    void clearError(ClientDetails clientDetails, Object adapter, String ip);

}