package com.simple.common.sms.service;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.simple.common.core.common.service.lock.LockService;
import com.simple.common.core.function.DefaultFunction;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.mp.common.enums.Status;
import com.simple.common.sms.common.dto.sysSmsCode.FindAllSysSmsCodeRequest;
import com.simple.common.sms.common.entity.sysSmsCode.SysSmsCode;
import com.simple.common.sms.common.process.CheckSmsProcess;
import com.simple.common.sms.common.properties.SmsProperties;
import com.simple.common.sms.common.service.SmsService;
import com.simple.common.sms.common.view.sysSmsCode.SysSmsCodeView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
public abstract class AbsSmsService implements SmsService {

    @Autowired
    private List<CheckSmsProcess> checkSmsProcessList;

    @Autowired
    private LockService lockService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SmsProperties smsProperties;

    @Autowired
    private SysSmsCodeView sysSmsCodeView;

    @Override
    public void sendCode(String mobile, String code, String sendType) {
        DefaultFunction function = () -> {
            checkSmsProcessList.forEach(process -> {
                if (process.getProcess().isExecute()) {
                    process.execution(mobile, code);
                }
            });
            sendTemplateParam(mobile, sendType, "{'code':'" + code + "'}");
        };
        lockService.lock(mobile, function);
    }

    @Override
    public void checkSms(String mobile, String code, String sendType) {

        Long increment = redisTemplate.opsForValue().increment(mobile);
        AssertUtils.notEmpty(increment, "请重试");

        //第一次进，添加过期时间
        if (increment == 1L) {
            redisTemplate.expire(mobile, smsProperties.getOutTime(), TimeUnit.SECONDS);
        }
        AssertUtils.isTrue(increment <= smsProperties.getErrorSum(), "超过最大重试次数，请重新获取验证码");

        //获取该用户
        List<SysSmsCode> list = sysSmsCodeView.findByTimeAndPhoneAndState(
                        new FindAllSysSmsCodeRequest().setPhone(mobile).setStatus(Status.NOT_USED).setSendType(sendType).setCode(code));
        AssertUtils.notEmpty(list, "没有发送消息");

        //校验过期时间
        SysSmsCode sysSmsCode = list.get(0);
        long between = DateUtil.between(sysSmsCode.getCreateTime(), DateUtil.date(), DateUnit.SECOND, true);
        AssertUtils.isTrue(between <= smsProperties.getOutTime(), "验证码已过期，请重新获取验证码");

        //判断验证码是否正确
        AssertUtils.isTrue(sysSmsCode.getCode().equals(code), "验证码错误，请重新输入");

        //设置为已使用
        sysSmsCode.setStatus(Status.USED);
        sysSmsCodeView.updateById(sysSmsCode);
    }
}
