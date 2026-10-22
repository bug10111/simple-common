package com.simple.common.sms.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created with IntelliJ IDEA
 * Description: 短信模块装配配置。
 * 使用 @AutoConfiguration 声明:参与自动配置排序(可用 @AutoConfigureBefore/After),
 * 且不再生成无意义的 CGLIB 代理;保持原有组件扫描与 Mapper 扫描行为不变。
 * 注意本配置需在 MybatisPlusConfig/DataSource 自动配置之后执行以完成 Mapper 装配。
 *
 * @author qty
 */
@AutoConfiguration
@ComponentScan(basePackages = { "com.simple.common.sms" })
@MapperScan(basePackages = { "com.simple.common.sms.view" })
public class SmsConfig {

}
