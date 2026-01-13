package com.simple.common.rabbitmq.common.config;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@EnableRabbit
@Configuration
@ComponentScan(basePackages = { "com.simple.common.rabbitmq" })
public class RabbitMqConfig {

}
