package com.simple.common.sms.common.service;

import com.simple.common.core.utils.IPUtils;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
public interface SmsService {

    /**
     * 发送短信验证码
     *
     * @param mobile   手机号
     * @param code     验证码
     * @param sendType 配置发送的短信类型
     */
    void sendCode(String mobile, String code, String sendType);

    /**
     * 发送短信
     *
     * @param mobile        手机号
     * @param sendType      短信类型
     * @param templateParam 参数模板
     * @param ip            IP地址
     */
    void sendTemplateParam(String mobile, String sendType, String templateParam, String ip);

    /**
     * 校验短信验证码
     *
     * @param mobile   手机号
     * @param code     验证码
     * @param sendType 配置发送的短信类型
     */
    void checkSms(String mobile, String code, String sendType);

    /**
     * 发送短信
     *
     * @param mobile        手机号
     * @param sendType      短信类型
     * @param templateParam 参数模板
     */
    default void sendTemplateParam(String mobile, String sendType, String templateParam) {
        sendTemplateParam(mobile, sendType, templateParam, IPUtils.getIpAddr());
    }
}
