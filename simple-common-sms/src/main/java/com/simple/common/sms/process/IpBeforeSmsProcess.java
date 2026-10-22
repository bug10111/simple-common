package com.simple.common.sms.process;

import cn.hutool.core.util.StrUtil;
import com.simple.common.core.common.enums.process.DefaultKindProcess;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.IPUtils;
import com.simple.common.sms.common.dto.sysSmsCode.FindAllSysSmsCodeRequest;
import com.simple.common.sms.common.enums.BeforeSmsKindProcess;
import com.simple.common.sms.common.process.CheckSmsProcess;
import com.simple.common.sms.common.properties.SmsProperties;
import com.simple.common.sms.common.view.sysSmsCode.SysSmsCodeView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA
 * Description: 验证码发送次数校验(IP 维度)
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
    public BeforeSmsKindProcess getProcess() {
        return BeforeSmsKindProcess.IP_PROCESS;
    }

    @Override
    public void execution(String phone, String code) {
        // 解析不到客户端 IP(非 Servlet 请求线程等)时直接拒绝,避免把无 IP 请求计入全局配额
        String ip = IPUtils.getIpAddr();
        AssertUtils.isTrue(StrUtil.isNotBlank(ip), "无法识别客户端IP，发送被拒绝");

        FindAllSysSmsCodeRequest findAllSysSmsCodeRequest = new FindAllSysSmsCodeRequest().setIp(ip).setDate(cn.hutool.core.date.DateUtil.date().toDateStr());
        // 仅统计行数,不再物化整批实体;严格小于:已存条数等于上限时本次请求即被拒绝(本次尚未落库)
        long count = sysSmsCodeView.count(findAllSysSmsCodeRequest);
        AssertUtils.isTrue(count < smsProperties.getIpSendMax(), "今日验证码发送次数已达上限");
    }
}
