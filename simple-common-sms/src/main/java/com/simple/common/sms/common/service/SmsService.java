package com.simple.common.sms.common.service;

import com.simple.common.core.utils.IPUtils;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
public interface SmsService {

    /**
     * 发送短信验证码
     * <p>
     * 发送前依次执行短信校验链(时间间隔/IP 次数/手机号次数),任一环节校验失败即拒绝发送;
     * 整个校验+发送流程以「发送类型+手机号」为粒度加锁,保证同一手机号同一类型不并发发送。
     * </p>
     *
     * @param mobile   手机号(不能为空)
     * @param code     验证码
     * @param sendType 短信类型配置标识(不能为空)
     * @throws RuntimeException 校验不通过(参数为空/发送频繁/超限)或网关发送失败时抛出异常
     */
    void sendCode(String mobile, String code, String sendType);

    /**
     * 发送模板短信(指定客户端IP)
     * <p>
     * 校验通过后调用短信网关,并把本次请求(含验证码原文与网关返回)落库,
     * 网关业务失败(签名/模板未过审、限流等)同样抛出异常,发送失败不会被静默掩盖。
     * </p>
     *
     * @param mobile        手机号(不能为空)
     * @param sendType      短信类型配置标识(不能为空)
     * @param templateParam 模板参数(JSON 格式,必须含 code 字段,缺失时发送被拒绝)
     * @param ip            客户端IP地址,用于防刷校验与发送记录留痕
     * @throws RuntimeException 当校验不通过、模板参数非法或网关发送失败时抛出异常
     */
    void sendTemplateParam(String mobile, String sendType, String templateParam, String ip);

    /**
     * 校验短信验证码
     * <p>
     * 语义契约(消费式校验)：
     * <ul>
     *   <li>校验通过即消费:验证码原子置为「已使用」,同一验证码并发重复校验只有一次成功;</li>
     *   <li>错误尝试计数:校验失败累计错误次数,超过 errorSum 次后在 outTime 秒内锁定手机号;</li>
     *   <li>有效期:验证码在 outTime 秒内有效,过期后校验失败;</li>
     *   <li>校验成功会清空错误计数器,合法校验不会互相消耗重试额度。</li>
     * </ul>
     * </p>
     *
     * @param mobile   手机号(不能为空)
     * @param code     验证码(不能为空)
     * @param sendType 短信类型配置标识(不能为空)
     * @throws RuntimeException 当验证码错误、已过期、超过最大重试次数或无发送记录时抛出异常
     */
    void checkSms(String mobile, String code, String sendType);

    /**
     * 发送模板短信(自动获取客户端IP)
     * <p>
     * 注意：客户端 IP 依赖当前线程的 Servlet 请求上下文,
     * 仅可在处理 HTTP 请求的线程中调用;定时任务/消息队列等异步场景请使用
     * {@link #sendTemplateParam(String, String, String, String)} 并自行传入可信 IP。
     * </p>
     *
     * @param mobile        手机号(不能为空)
     * @param sendType      短信类型配置标识(不能为空)
     * @param templateParam 模板参数(JSON 格式)
     * @throws RuntimeException 当校验不通过或网关发送失败时抛出异常
     */
    default void sendTemplateParam(String mobile, String sendType, String templateParam) {
        sendTemplateParam(mobile, sendType, templateParam, IPUtils.getIpAddr());
    }
}
