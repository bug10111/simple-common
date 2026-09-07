package com.simple.common.sms.service;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.simple.common.core.common.service.lock.LockService;
import com.simple.common.core.function.DefaultFunction;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.mp.common.enums.Status;
import com.simple.common.sms.common.dto.sysSmsCode.FindAllSysSmsCodeRequest;
import com.simple.common.sms.common.entity.sysSmsCode.SysSmsCode;
import com.simple.common.sms.common.process.CheckSmsProcess;
import com.simple.common.sms.common.properties.SmsProperties;
import com.simple.common.sms.common.service.SmsService;
import com.simple.common.sms.common.view.sysSmsCode.SysSmsCodeView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 短信服务抽象基类。
 * <p>
 * 实现 {@link SmsService} 接口，提供短信发送前的校验逻辑和配置属性注入。
 * 具体的短信平台实现类应继承此基类，实现 {@link #sendTemplateParam(String, String, String)} 方法。
 * </p>
 *
 * <h3>扩展指南：</h3>
 * <p>
 * 如需对接新的短信平台，可继承此类并实现 {@code sendTemplateParam} 方法：
 * </p>
 * <pre>{@code
 * public class TencentSmsService extends AbsSmsService {
 *     @Override
 *     protected void sendTemplateParam(String mobile, String sendType, String templateParam) {
 *         // 调用腾讯云短信API发送短信
 *     }
 * }
 * }</pre>
 *
 * <h3>框架默认实现：</h3>
 * <p>
 * {@link AliSmsService} - 阿里云短信服务实现
 * </p>
 *
 * @author qty
 */
public abstract class AbsSmsService implements SmsService {

    @Autowired
    private List<CheckSmsProcess> checkSmsProcessList;

    @Autowired
    private LockService lockService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 短信配置属性，自动注入
     */
    @Autowired
    protected SmsProperties smsProperties;

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
            // 构建标准双引号 JSON 模板参数，符合短信平台变量模板参数规范
            Map<String, String> templateParamMap = Collections.singletonMap("code", code);
            sendTemplateParam(mobile, sendType, JsonUtils.toJsonStr(templateParamMap));
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

        //查询该手机号该类型最新一条未使用验证码记录（不携带 code 条件，避免错误输入导致记录查不到）
        List<SysSmsCode> list = sysSmsCodeView.findByTimeAndPhoneAndState(
                        new FindAllSysSmsCodeRequest().setPhone(mobile).setStatus(Status.NOT_USED).setSendType(sendType));
        AssertUtils.notEmpty(list, "没有发送消息或验证码已过期");

        //校验验证码是否过期
        SysSmsCode sysSmsCode = list.get(0);
        long between = DateUtil.between(sysSmsCode.getCreateTime(), DateUtil.date(), DateUnit.SECOND, true);
        AssertUtils.isTrue(between <= smsProperties.getOutTime(), "验证码已过期，请重新获取验证码");

        //比对验证码，不一致提示验证码错误
        AssertUtils.isTrue(sysSmsCode.getCode().equals(code), "验证码错误，请重新输入");

        //设置为已使用
        sysSmsCode.setStatus(Status.USED);
        sysSmsCodeView.updateById(sysSmsCode);
    }
}