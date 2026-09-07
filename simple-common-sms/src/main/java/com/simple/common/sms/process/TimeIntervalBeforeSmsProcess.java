package com.simple.common.sms.process;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
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

import java.util.Comparator;
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
            SysSmsCode sysSmsCode = findLatestSendRecord(all);
            Date begin = sysSmsCode.getCreateTime();
            Date end = DateUtil.date();

            // 校验距上次发送是否已超过最低间隔（time-inter 单位为秒），防止验证码轰炸
            long between = DateUtil.between(begin, end, DateUnit.SECOND, true);
            AssertUtils.isTrue(between > smsProperties.getTimeInter(), "验证码发送频繁，请{}秒后再试", smsProperties.getTimeInter());
        }
    }

    /**
     * 取当日最新一条发送记录作为时间间隔基准
     * 列表查询无固定排序，直接取首条可能拿到更早记录，导致间隔校验被绕过
     *
     * @param records 当日该手机号的发送记录
     * @return 创建时间最新的一条发送记录
     */
    private SysSmsCode findLatestSendRecord(List<SysSmsCode> records) {
        // 创建时间由新增时自动填充，此处过滤空值保证比较安全
        return records.stream()
                      .filter(record -> ObjUtil.isNotEmpty(record.getCreateTime()))
                      .max(Comparator.comparing(SysSmsCode::getCreateTime))
                      .orElse(records.get(0));
    }
}
