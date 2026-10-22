package com.simple.common.sms.process;

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

import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: 验证码发送时间间隔校验
 * <p>
 * 间隔基准取「最近一条发送记录」而非「当日记录」：按自然日过滤会在零点边界产生漏洞
 * （昨日 23:59:30 发出、今日 00:00:10 再次请求时读不到当日记录,间隔校验被绕过）。
 * 这里改为按手机号取近一天内的记录,跨零点同样生效。
 * </p>
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
    public BeforeSmsKindProcess getProcess() {
        return BeforeSmsKindProcess.TIME_INTERVAL_PROCESS;
    }

    @Override
    public void execution(String phone, String code) {
        Date now = DateUtil.date();
        // 只需近一天窗口即可判定间隔(time-inter 上限远小于一天),避免全历史加载
        Date windowStart = cn.hutool.core.date.DateUtil.offsetDay(now, -1);
        FindAllSysSmsCodeRequest findAllSysSmsCodeRequest = new FindAllSysSmsCodeRequest().setPhone(phone);
        List<SysSmsCode> all = sysSmsCodeView.list(findAllSysSmsCodeRequest);

        SysSmsCode latest = findLatestSendRecord(all, windowStart);
        if (latest != null) {
            Date begin = latest.getCreateTime();
            Date end = now;

            // 非绝对值比较:时钟回拨/脏数据产生"未来时间"时应拒绝而非当作超长间隔放行;
            // 边界语义为"间隔至少 time-inter 秒",恰好等于阈值即允许再次发送(>=)
            long between = DateUtil.between(begin, end, cn.hutool.core.date.DateUnit.SECOND, false);
            AssertUtils.isTrue(between >= 0, "验证码发送频繁，请{}秒后再试", smsProperties.getTimeInter());
            AssertUtils.isTrue(between >= smsProperties.getTimeInter(), "验证码发送频繁，请{}秒后再试", smsProperties.getTimeInter());
        }
    }

    /**
     * 取时间窗口内最新一条发送记录作为间隔基准
     *
     * @param records     该手机号的发送记录
     * @param windowStart 窗口起点(含)
     * @return 窗口内创建时间最新的一条记录;窗口内无有效记录(createTime 为空的脏数据不计)返回 null
     */
    private SysSmsCode findLatestSendRecord(List<SysSmsCode> records, Date windowStart) {
        // createTime 由新增时自动填充,此处过滤空值保证比较安全;不再回退 get(0),避免把脏数据当基准
        return records.stream()
                      .filter(record -> record.getCreateTime() != null && !record.getCreateTime().before(windowStart))
                      .max(Comparator.comparing(SysSmsCode::getCreateTime))
                      .orElse(null);
    }
}
