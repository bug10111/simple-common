package com.simple.common.oauth.start.common.service;

import com.simple.common.oauth.start.common.dto.CheckSmsRequest;
import com.simple.common.oauth.start.common.dto.SendSmsRequest;

/**
 * Created with IntelliJ IDEA
 * Description: 短信验证码接口
 *
 * @author 兄台丶请冷静
 */
public interface SmsService {

    /**
     * 发送短信验证码
     *
     * @param request 参数
     */
    void send(SendSmsRequest request);

    /**
     * 校验短信验证码
     *
     * @param request 参数
     */
    void checkSms(CheckSmsRequest request);
}
