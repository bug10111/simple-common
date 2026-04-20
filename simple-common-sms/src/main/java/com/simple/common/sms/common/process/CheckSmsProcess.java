package com.simple.common.sms.common.process;

import com.simple.common.core.common.enums.process.DefaultKindProcess;
import com.simple.common.core.common.service.process.BasProcessService;

/**
 * 短信发送前校验处理接口。
 * <p>
 * 用于在短信发送前进行各种校验，如时间间隔校验、IP校验、手机号校验等。
 * 通过责任链模式，可以灵活组合多种校验逻辑。
 * </p>
 *
 * <h3>责任链配置：</h3>
 * <p>
 * 校验处理器的执行顺序由 {@link com.simple.common.sms.common.enums.BeforeSmsKindProcess} 枚举定义。
 * 实现类需要实现 {@link #getProcess()} 方法返回对应的枚举值。
 * </p>
 *
 * <h3>实现示例：</h3>
 * <pre>{@code
 * @Component
 * public class PhoneBeforeSmsProcess implements CheckSmsProcess {
 *     @Override
 *     public DefaultKindProcess getProcess() {
 *         return BeforeSmsKindProcess.PHONE_PROCESS;
 *     }
 *
 *     @Override
 *     public void execution(String phone, String code) {
 *         // 手机号校验逻辑
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
public interface CheckSmsProcess extends BasProcessService {

    /**
     * 获取对应的处理器类型枚举。
     * <p>
     * 返回 {@link com.simple.common.sms.common.enums.BeforeSmsKindProcess} 枚举值，
     * 用于确定处理器的执行顺序和是否执行。
     * </p>
     *
     * @return 处理器类型枚举
     */
    DefaultKindProcess getProcess();

    /**
     * 执行短信发送前的校验逻辑。
     * <p>
     * 根据业务需求实现具体的校验逻辑，如校验发送时间间隔、校验IP发送次数、校验手机号发送次数等。
     * </p>
     *
     * @param phone 手机号
     * @param code 验证码
     * @throws com.simple.common.core.exception.DefaultException 当校验失败时抛出异常
     */
    void execution(String phone, String code);

}