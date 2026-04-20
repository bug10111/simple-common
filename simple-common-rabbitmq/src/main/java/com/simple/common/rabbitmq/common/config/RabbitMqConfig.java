package com.simple.common.rabbitmq.common.config;

import com.simple.common.rabbitmq.common.entity.EnhancedCorrelationData;
import com.simple.common.rabbitmq.common.manager.SendFailurePersistenceManager;
import com.simple.common.rabbitmq.common.properties.RabbitMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 延迟重试配置
 * 使用 RabbitMQ 延迟交换机插件（rabbitmq_delayed_message_exchange）
 *
 * @author qty
 */
@Slf4j
@EnableRabbit
@Configuration
@ComponentScan(basePackages = { "com.simple.common.rabbitmq" })
public class RabbitMqConfig {

    @Autowired
    private SendFailurePersistenceManager sendFailurePersistenceManager;

    @Autowired
    private RabbitMqProperties rabbitMqProperties;

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        // 使用 Jackson JSON 转换器
        template.setMessageConverter(new Jackson2JsonMessageConverter());

        // 发布确认回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // 尝试从 EnhancedCorrelationData 中提取完整信息
                if (correlationData instanceof EnhancedCorrelationData enhancedData) {
                    sendFailurePersistenceManager.saveConfirmFailure(enhancedData.getId(), cause, enhancedData.getExchange(), enhancedData.getRoutingKey(), enhancedData.getDefaultMessage(),
                                                                     enhancedData.getMessageBody());
                } else {
                    // 降级处理：仅记录 ID
                    String id = correlationData != null ? correlationData.getId() : null;
                    sendFailurePersistenceManager.saveConfirmFailure(id, cause, null, null, null, null);
                    if (correlationData == null) {
                        log.warn("ConfirmCallback 中 correlationData 为 null，无法记录失败消息详情");
                    }
                }
            }
        });

        // 路由失败回调
        template.setReturnsCallback(returned -> {
            Message message = returned.getMessage();
            String exchange = returned.getExchange();

            // 检查是否为延迟交换机（你框架中有多种延迟交换机，建议根据名称模式判断）
            if (exchange != null && (exchange.equals(rabbitMqProperties.getDelayedExchange()) || exchange.contains("delayed") || exchange.contains("delay"))) {
                return;
            }

            // 普通交换机触发 return，是真正的路由异常，必须持久化
            sendFailurePersistenceManager.saveReturnFailure(message, returned.getReplyCode(), returned.getReplyText(), exchange, returned.getRoutingKey());
        });

        // 必须设置为 true，否则 ReturnCallback 不生效
        template.setMandatory(true);

        return template;
    }

    /**
     * 定义延迟交换机，类型为 x-delayed-message
     * 使用 CustomExchange 来声明，底层消息路由类型为 direct
     */
    @Bean("simpleDelayedRetryExchange")
    public Exchange delayedExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(rabbitMqProperties.getDelayedExchange(), "x-delayed-message", true, false, args);
    }
}