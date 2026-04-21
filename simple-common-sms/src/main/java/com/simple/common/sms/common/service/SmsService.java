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
     *
     * @param mobile   手机号
     * @param code     验证码
     * @param sendType 短信类型配置标识
     * @throws RuntimeException 当短信发送失败时抛出异常
     */
    void sendCode(String mobile, String code, String sendType);

    /**
     * 发送模板短信
     *
     * @param mobile        手机号
     * @param sendType      短信类型配置标识
     * @param templateParam 模板参数（JSON格式）
     * @param ip            客户端IP地址，用于防刷校验
     * @throws RuntimeException 当短信发送失败或校验不通过时抛出异常
     */
    void sendTemplateParam(String mobile, String sendType, String templateParam, String ip);

    /**
     * 校验短信验证码
     *
     * @param mobile   手机号
     * @param code     验证码
     * @param sendType 短信类型配置标识
     * @throws RuntimeException 当验证码错误或已过期时抛出异常
     */
    void checkSms(String mobile, String code, String sendType);

    /**
     * 发送模板短信（自动获取客户端IP）
     *
     * @param mobile        手机号
     * @param sendType      短信类型配置标识
     * @param templateParam 模板参数（JSON格式）
     * @throws RuntimeException 当短信发送失败或校验不通过时抛出异常
     */
    default void sendTemplateParam(String mobile, String sendType, String templateParam) {
        sendTemplateParam(mobile, sendType, templateParam, IPUtils.getIpAddr());
    }
}
