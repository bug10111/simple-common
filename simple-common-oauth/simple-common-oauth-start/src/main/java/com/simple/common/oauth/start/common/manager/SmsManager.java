package com.simple.common.oauth.start.common.manager;

import com.simple.common.oauth.start.common.dto.CheckSmsRequest;
import com.simple.common.oauth.start.common.dto.SendSmsRequest;

/**
 * Created with IntelliJ IDEA
 * Description: 短信验证码实现
 *
 * @author 兄台丶请冷静
 */
public interface SmsManager {

    /**
     * 发送短信验证码
     *
     * @param request 请求参数
     */
    void send(SendSmsRequest request);

    /**
     * 校验短信验证码
     *
     * @param request 请求参数
     */
    void checkSms(CheckSmsRequest request);
}
