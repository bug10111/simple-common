package com.simple.common.sms.common.properties;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Created with IntelliJ IDEA
 * Description: 短信发送配置类
 * <p>
 * 全部数值型限制均要求正数：这些值直接驱动限流阈值与验证码有效期,
 * 配置为 0 或负数会导致限流失真、验证码秒过期,必须在装配阶段快速失败;
 * 如确有关闭某项限制的诉求,应显式改造代码而非把阈值配成 0。
 * </p>
 *
 * @author qty
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "simple.ali.sms")
public class SmsProperties {

    //一天ip发送短信最大次数(必须为正)
    @Min(value = 1, message = "simple.ali.sms.ip-send-max 必须为正数")
    private int ipSendMax = 20;

    //一天相同手机号发送短信最大次数(必须为正)
    @Min(value = 1, message = "simple.ali.sms.phone-send-max 必须为正数")
    private int phoneSendMax = 5;

    //发送最低时间间隔（单位：秒,必须为正）
    @Min(value = 1, message = "simple.ali.sms.time-inter 必须为正数")
    private int timeInter = 60;

    //验证码超时时间（单位：秒,必须为正）
    @Min(value = 1, message = "simple.ali.sms.out-time 必须为正数")
    private int outTime = 300;

    //每次短信验证码允许的错误验证次数(必须为正)
    @Min(value = 1, message = "simple.ali.sms.error-sum 必须为正数")
    private int errorSum = 3;

    //服务地址(保留 dysmsapi.aliyuncs.com 公网默认值:未配置时静默走公网端点,
    //多环境部署请在各环境配置文件中显式覆盖该值)
    private String endpoint = "dysmsapi.aliyuncs.com";
}
