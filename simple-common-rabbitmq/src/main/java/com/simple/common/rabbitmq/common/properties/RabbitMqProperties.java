package com.simple.common.rabbitmq.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.mq.rabbitmq")
public class RabbitMqProperties {

    //rabbitMQ相关储存信息的总包名
    private String mqPackage = "rabbitmq:";

    //是否消费
    private String whetherToConsume = mqPackage + "whether_to_consume";

    //消费计数
    private String calculation = mqPackage + "calculation";

    //发送消息备份
    private String sendBackup = mqPackage + "send_backup";

    //持久化失败
    private String sendError = mqPackage + "send_error";

}
