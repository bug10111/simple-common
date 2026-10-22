package com.simple.common.sms.common.process;

import com.simple.common.core.common.enums.process.DefaultKindProcess;
import com.simple.common.core.common.service.process.BasProcessService;
import com.simple.common.sms.common.enums.BeforeSmsKindProcess;

/**
 * 短信发送前校验处理接口。
 * <p>
 * 用于在短信发送前进行各种校验，如时间间隔校验、IP校验、手机号校验等。
 * 通过责任链模式，可以灵活组合多种校验逻辑。
 * </p>
 *
 * <h3>责任链契约：</h3>
 * <p>
 * 校验处理器的执行顺序由 {@link BeforeSmsKindProcess} 枚举定义。
 * 链式调用语义：实现方校验不通过时必须抛出业务异常（如 DefaultException）中断整个发送流程;
 * 正常返回表示校验通过、继续执行后续处理器;实现方自身不应执行发送动作或产生副作用。
 * DefaultException 为 RuntimeException，无需在签名上声明 throws。
 * </p>
 *
 * <h3>实现示例：</h3>
 * <pre>{@code
 * @Component
 * public class PhoneBeforeSmsProcess implements CheckSmsProcess {
 *     @Override
 *     public BeforeSmsKindProcess getProcess() {
 *         return BeforeSmsKindProcess.PHONE_PROCESS;
 *     }
 *
 *     @Override
 *     public void execution(String phone, String code) {
 *         // 手机号校验逻辑,不通过时抛出 DefaultException
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
     * 收窄返回类型为 {@link BeforeSmsKindProcess}：实现方只能返回本模块的短信阶段枚举值，
     * 杜绝返回无关枚举导致执行顺序/开关语义失真。
     * </p>
     *
     * @return 短信阶段处理器枚举
     */
    @Override
    BeforeSmsKindProcess getProcess();

    /**
     * 执行短信发送前的校验逻辑。
     * <p>
     * 根据业务需求实现具体的校验逻辑，如校验发送时间间隔、校验IP发送次数、校验手机号发送次数等。
     * </p>
     *
     * @param phone 手机号
     * @param code  验证码
     * @throws com.simple.common.core.exception.DefaultException 当校验失败时抛出异常(中断整个发送流程)
     */
    void execution(String phone, String code);

}
