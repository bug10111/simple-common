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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, List<Exchange> exchangeBeans) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        // 使用 Jackson JSON 转换器
        template.setMessageConverter(new Jackson2JsonMessageConverter());

        // 收集延迟交换机名称集合，Return 回调据此按声明类型精确判定
        Set<String> delayedExchangeNames = collectDelayedExchangeNames(exchangeBeans);

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

            // 延迟交换机的 return 由延迟插件内部处理，直接忽略；判定口径为交换机声明的类型参数（x-delayed-message），不按名称模糊匹配
            if (exchange != null && delayedExchangeNames.contains(exchange)) {
                return;
            }

            // 其余交换机触发 return 均为真实路由失败，必须持久化
            sendFailurePersistenceManager.saveReturnFailure(message, returned.getReplyCode(), returned.getReplyText(), exchange, returned.getRoutingKey());
        });

        // 必须设置为 true，否则 ReturnCallback 不生效
        template.setMandatory(true);

        return template;
    }

    /**
     * 收集延迟交换机名称集合
     * <p>
     * 判定口径：按交换机声明时的类型参数（x-delayed-message）精确识别，不按交换机名称模糊匹配，
     * 避免业务交换机名称含 delay/delayed 字样时其 return 被误当延迟忽略而丢消息；
     * 配置项 delayedExchange 指向的延迟重试交换机一并纳入，覆盖未以 Bean 形式声明的场景。
     * </p>
     *
     * @param exchangeBeans 容器内全部交换机 Bean，用于按声明类型参数（x-delayed-message）精确识别延迟交换机
     * @return 延迟交换机名称集合
     */
    private Set<String> collectDelayedExchangeNames(List<Exchange> exchangeBeans) {
        Set<String> delayedExchangeNames = new HashSet<>();
        if (exchangeBeans != null) {
            // 遍历容器内交换机 Bean，取声明类型为 x-delayed-message 的交换机名称
            for (Exchange exchangeBean : exchangeBeans) {
                if ("x-delayed-message".equals(exchangeBean.getType())) {
                    delayedExchangeNames.add(exchangeBean.getName());
                }
            }
        }
        // 配置声明的延迟重试交换机纳入判定集合（精确名称）
        delayedExchangeNames.add(rabbitMqProperties.getDelayedExchange());
        return delayedExchangeNames;
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