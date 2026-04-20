package com.simple.common.auth.server.common.process;

import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.entity.TokenData;
import com.simple.common.core.common.service.process.BasProcessService;

/**
 * 登录成功处理接口。
 * <p>
 * 继承 {@link BasProcessService}，用于实现登录成功后的处理逻辑。
 * 通过责任链模式，多个成功处理器可以按顺序执行，实现日志记录、用户信息保存等
 * 多种成功处理机制的联合处理。
 * </p>
 *
 * <h3>责任链配置：</h3>
 * <p>
 * 成功处理器的执行顺序通过 {@link com.simple.common.auth.server.common.enums.process.LoginSucKindProcess}
 * 枚举定义，框架默认提供以下处理器（按执行顺序）：</p>
 * <ol>
 *   <li>{@link com.simple.common.auth.server.process.SaveInfoLoginSucProcess} - 保存用户登录信息到Redis</li>
 *   <li>{@link com.simple.common.auth.server.process.LogLoginSucProcess} - 记录登录成功日志</li>
 * </ol>
 *
 * <h3>自定义扩展：</h3>
 * <p>
 * 如需添加自定义成功处理逻辑（如发送登录通知、积分奖励等），可实现此接口并注册到
 * {@link com.simple.common.auth.server.common.enums.process.LoginSucKindProcess} 枚举中。
 * </p>
 *
 * @author qty
 */
public interface LoginSucProcess extends BasProcessService {

    /**
     * 执行权限过滤器
     *
     * @param tokenData 登录中token数据对象
     */
    void execute(TokenData tokenData);
}