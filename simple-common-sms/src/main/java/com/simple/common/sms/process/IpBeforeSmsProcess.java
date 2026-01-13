package com.simple.common.sms.process;

import cn.hutool.core.date.DateUtil;
import com.simple.common.core.common.enums.process.DefaultKindProcess;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.IPUtils;
import com.simple.common.sms.common.dto.sysSmsCode.FindAllSysSmsCodeRequest;
import com.simple.common.sms.common.entity.sysSmsCode.SysSmsCode;
import com.simple.common.sms.common.enums.BeforeSmsKindProcess;
import com.simple.common.sms.common.process.CheckSmsProcess;
import com.simple.common.sms.common.properties.SmsProperties;
import com.simple.common.sms.common.view.sysSmsCode.SysSmsCodeView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: 验证码发送次数校验
 *
 * @author qty
 */
@Service
public class IpBeforeSmsProcess implements CheckSmsProcess {

    @Autowired
    private SysSmsCodeView sysSmsCodeView;

    @Autowired
    private SmsProperties smsProperties;

    @Override
    public DefaultKindProcess getProcess() {
        return BeforeSmsKindProcess.IP_PROCESS;
    }

    @Override
    public void execution(String phone, String code) {
        FindAllSysSmsCodeRequest findAllSysSmsCodeRequest = new FindAllSysSmsCodeRequest().setIp(IPUtils.getIpAddr()).setDate(DateUtil.date().toDateStr());
        List<SysSmsCode> all = sysSmsCodeView.list(findAllSysSmsCodeRequest);
        AssertUtils.isTrue(all.size() <= smsProperties.getIpSendMax(), "今日验证码发送次数已达上限");
    }
}
