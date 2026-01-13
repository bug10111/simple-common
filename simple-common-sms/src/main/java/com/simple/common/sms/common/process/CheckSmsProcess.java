package com.simple.common.sms.common.process;

import com.simple.common.core.common.service.process.BasProcessService;

/**
 * Created with IntelliJ IDEA
 * Description: 定义短信验证码校验接口
 *
 * @author qty
 */
public interface CheckSmsProcess extends BasProcessService {

    /**
     * 执行流程
     *
     * @param phone 手机号
     * @param code  验证码
     */
    void execution(String phone, String code);

}
