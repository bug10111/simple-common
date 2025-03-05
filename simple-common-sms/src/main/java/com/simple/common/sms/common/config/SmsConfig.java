package com.simple.common.sms.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Configuration
@ComponentScan(basePackages = { "com.simple.common.sms" })
@MapperScan(basePackages = { "com.simple.common.sms.view" })
public class SmsConfig {

}
