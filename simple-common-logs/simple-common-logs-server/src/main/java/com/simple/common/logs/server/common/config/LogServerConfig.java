package com.simple.common.logs.server.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Configuration
@ComponentScan(basePackages = "com.simple.common.logs.server")
@MapperScan(basePackages = { "com.simple.common.logs.server.view" })
public class LogServerConfig {}
