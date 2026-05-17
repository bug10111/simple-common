package com.simple.common.sms.process;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.simple.common.core.common.enums.process.DefaultKindProcess;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.sms.common.dto.sysSmsCode.FindAllSysSmsCodeRequest;
import com.simple.common.sms.common.entity.sysSmsCode.SysSmsCode;
import com.simple.common.sms.common.enums.BeforeSmsKindProcess;
import com.simple.common.sms.common.process.CheckSmsProcess;
import com.simple.common.sms.common.properties.SmsProperties;
import com.simple.common.sms.common.view.sysSmsCode.SysSmsCodeView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description:验证码发送时间间隔校验
 *
 * @author qty
 */
@Service
public class TimeIntervalBeforeSmsProcess implements CheckSmsProcess {

    @Autowired
    private SysSmsCodeView sysSmsCodeView;

    @Autowired
    private SmsProperties smsProperties;

    @Override
    public DefaultKindProcess getProcess() {
        return BeforeSmsKindProcess.TIME_INTERVAL_PROCESS;
    }

    @Override
    public void execution(String phone, String code) {
        FindAllSysSmsCodeRequest findAllSysSmsCodeRequest = new FindAllSysSmsCodeRequest().setPhone(phone).setDate(DateUtil.date().toDateStr());
        List<SysSmsCode> all = sysSmsCodeView.list(findAllSysSmsCodeRequest);
        if (!all.isEmpty()) {
            SysSmsCode sysSmsCode = all.get(0);
            Date begin = sysSmsCode.getCreateTime();
            Date end = DateUtil.date();

            long between = DateUtil.between(begin, end, DateUnit.MS, true);
            AssertUtils.isTrue(between > smsProperties.getTimeInter(), "验证码发送频繁，请{}秒后再试", smsProperties.getTimeInter());
        }
    }
}
