package com.simple.common.sms.process;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.simple.common.core.common.enums.process.DefaultKindProcess;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.sms.common.dto.sysSmsCode.FindAllSysSmsCodeRequest;
import com.simple.common.sms.common.enums.BeforeSmsKindProcess;
import com.simple.common.sms.common.process.CheckSmsProcess;
import com.simple.common.sms.common.properties.SmsProperties;
import com.simple.common.sms.common.view.sysSmsCode.SysSmsCodeView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA
 * Description: 验证码发送次数校验(手机号维度)
 *
 * @author qty
 */
@Service
public class PhoneBeforeSmsProcess implements CheckSmsProcess {

    @Autowired
    private SysSmsCodeView sysSmsCodeView;

    @Autowired
    private SmsProperties smsProperties;

    @Override
    public BeforeSmsKindProcess getProcess() {
        return BeforeSmsKindProcess.PHONE_PROCESS;
    }

    @Override
    public void execution(String phone, String code) {
        // 手机号为空时 eq 条件会被查询构造器忽略,导致按全表计数误判上限,必须前置校验
        AssertUtils.isTrue(StrUtil.isNotBlank(phone), "手机号不能为空");

        FindAllSysSmsCodeRequest findAllSysSmsCodeRequest = new FindAllSysSmsCodeRequest().setPhone(phone).setDate(DateUtil.date().toDateStr());
        // 仅统计行数,不物化实体;严格小于:已存条数等于上限时本次请求即被拒绝(本次尚未落库)
        long count = sysSmsCodeView.count(findAllSysSmsCodeRequest);
        AssertUtils.isTrue(count < smsProperties.getPhoneSendMax(), "今日发送验证码次数已达上限，请明日再试");
    }
}
