package com.simple.common.sms.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: minio配置类
 *
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.ali.sms")
public class SmsProperties {

    //一天ip发送短信最大次数
    private int ipSendMax = 20;

    //一天相同手机号发送短信最大次数
    private int phoneSendMax = 5;

    //发送最低时间间隔
    private int timeInter = 60;

    //验证码超时时间
    private int outTime = 300;

    //每次短信验证码允许的错误验证次数
    private int errorSum = 3;

    //服务地址
    private String endpoint = "dysmsapi.aliyuncs.com";
}
