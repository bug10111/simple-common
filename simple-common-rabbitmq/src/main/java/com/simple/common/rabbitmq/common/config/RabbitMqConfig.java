package com.simple.common.rabbitmq.common.config;

import com.simple.common.rabbitmq.common.entity.EnhancedCorrelationData;
import com.simple.common.rabbitmq.common.manager.SendFailurePersistenceManager;
import com.simple.common.rabbitmq.service.DefaultDeadLetterService;
import com.simple.common.rabbitmq.manager.DefaultSendFailurePersistenceManager;
import com.simple.common.rabbitmq.common.service.DeadLetterService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import com.simple.common.rabbitmq.common.properties.RabbitMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 延迟重试配置
 * 使用 RabbitMQ 延迟交换机插件（rabbitmq_delayed_message_exchange）
 * <p>
 * 组件扫描只列举框架自身的子包（不再整包通配），避免无差别接管业务方放在
 * com.simple.common.rabbitmq 包下的自定义 Bean。
 * </p>
 *
 * @author qty
 */
@Slf4j
@EnableRabbit
@Configuration
@ComponentScan(basePackages = {
        "com.simple.common.rabbitmq.manager",
        "com.simple.common.rabbitmq.service",
        "com.simple.common.rabbitmq.aspeck",
        "com.simple.common.rabbitmq.common.config",
        "com.simple.common.rabbitmq.common.manager",
        "com.simple.common.rabbitmq.common.properties",
        "com.simple.common.rabbitmq.common.service"
})
public class RabbitMqConfig {

    /**
     * 发送失败持久化管理器（业务自定义实现优先，默认本地文件兜底实现次之）
     */
    @Autowired
    private ObjectProvider<SendFailurePersistenceManager> sendFailurePersistenceManagerProvider;

    /**
     * 默认发送失败持久化管理器类型（用于多 Bean 场景识别默认实现）
     */
    private final Class<?> defaultPersistenceManagerType = DefaultSendFailurePersistenceManager.class;

    @Autowired
    private RabbitMqProperties rabbitMqProperties;

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, List<Exchange> exchangeBeans) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        // 使用 Jackson JSON 转换器（Content-Type 由转换器统一写入 application/json）
        template.setMessageConverter(new Jackson2JsonMessageConverter());

        // 收集延迟交换机名称集合，Return 回调据此按声明类型精确判定
        Set<String> delayedExchangeNames = collectDelayedExchangeNames(exchangeBeans);

        // 发布确认回调（异常自隔离：持久化动作异常不得中断确认派发、不得击穿通道）
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                try {
                    // 尝试从 EnhancedCorrelationData 中提取完整信息
                    if (correlationData instanceof EnhancedCorrelationData enhancedData) {
                        resolveSendFailurePersistenceManager()
                                .saveConfirmFailure(enhancedData.getId(), cause, enhancedData.getExchange(), enhancedData.getRoutingKey(), enhancedData.getDefaultMessage(),
                                        enhancedData.getMessageBody());
                    } else {
                        // 降级处理：仅记录 ID
                        String id = correlationData != null ? correlationData.getId() : null;
                        resolveSendFailurePersistenceManager().saveConfirmFailure(id, cause, null, null, null, null);
                        if (correlationData == null) {
                            log.warn("ConfirmCallback 中 correlationData 为 null，无法记录失败消息详情");
                        }
                    }
                } catch (Exception e) {
                    // 失败持久化抛出的任何异常都仅记录日志：回调运行在确认派发线程，
                    // 异常外泄会中断剩余待确认派发甚至导致通道关闭
                    log.error("处理发送确认失败回调异常，correlationData={}", correlationData, e);
                }
            }
        });

        // 路由失败回调（异常自隔离）
        template.setReturnsCallback(returned -> {
            try {
                Message message = returned.getMessage();
                String exchange = returned.getExchange();

                // 延迟交换机的 return 由延迟插件内部处理，直接忽略；判定口径为交换机声明的类型参数（x-delayed-message），不按名称模糊匹配
                if (exchange != null && delayedExchangeNames.contains(exchange)) {
                    // 被忽略的 return 必须留痕，防止插件被卸载/交换机类型被改后消息静默丢失无从排查
                    log.warn("忽略延迟交换机 return（由延迟插件投递），exchange={}, routingKey={}, replyCode={}, replyText={}",
                            exchange, returned.getRoutingKey(), returned.getReplyCode(), returned.getReplyText());
                    return;
                }

                // 其余交换机触发 return 均为真实路由失败，必须持久化
                resolveSendFailurePersistenceManager()
                        .saveReturnFailure(message, returned.getReplyCode(), returned.getReplyText(), exchange, returned.getRoutingKey());
            } catch (Exception e) {
                log.error("处理消息路由失败回调异常，exchange={}, routingKey={}", returned.getExchange(), returned.getRoutingKey(), e);
            }
        });

        // 必须设置为 true，否则 ReturnCallback 不生效
        template.setMandatory(true);

        return template;
    }

    /**
     * 解析发送失败持久化管理器：业务自定义实现优先，存在多个非默认实现且无法裁决时回退默认兜底实现，
     * 保证确认/路由回调永不因 Bean 歧义启动失败或 NPE。
     */
    private SendFailurePersistenceManager resolveSendFailurePersistenceManager() {
        SendFailurePersistenceManager unique = sendFailurePersistenceManagerProvider.getIfUnique();
        if (unique != null) {
            return unique;
        }
        // 多个实现：优先业务自定义实现，找不到再退回默认兜底实现
        SendFailurePersistenceManager userDefined = sendFailurePersistenceManagerProvider.stream()
                .filter(manager -> !defaultPersistenceManagerType.isInstance(manager))
                .findFirst().orElse(null);
        if (userDefined != null) {
            return userDefined;
        }
        return sendFailurePersistenceManagerProvider.stream()
                .filter(defaultPersistenceManagerType::isInstance)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到任何 SendFailurePersistenceManager 实现"));
    }

    /**
     * 收集延迟交换机名称集合
     * <p>
     * 判定口径：按交换机声明时的类型参数（x-delayed-message）精确识别，不按交换机名称模糊匹配，
     * 避免业务交换机名称含 delay/delayed 字样时其 return 被误当延迟忽略而丢消息；
     * 配置项 delayedExchange 指向的延迟重试交换机一并纳入，覆盖未以 Bean 形式声明的场景
     * （配置为空白属于错误配置——空白名会被解析为 RabbitMQ 默认交换机，启动阶段直接失败）。
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
        // 配置声明的延迟重试交换机纳入判定集合（精确名称），空白配置快速失败
        String configuredDelayedExchange = rabbitMqProperties.getDelayedExchange();
        if (!StringUtils.hasText(configuredDelayedExchange)) {
            throw new IllegalStateException("配置项 simple.mq.rabbitmq.delayed-exchange 不允许为空白");
        }
        delayedExchangeNames.add(configuredDelayedExchange);
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

    /**
     * 默认死信服务注册：集成方已声明自定义死信服务 Bean 时不注册默认实现，
     * 避免 NoUniqueBeanDefinitionException 启动失败，使"自定义替换"真正可用
     */
    @Bean
    @ConditionalOnMissingBean(DeadLetterService.class)
    public DeadLetterService deadLetterService(RabbitMqProperties RabbitMqPropertiesValue) {
        return new DefaultDeadLetterService(RabbitMqPropertiesValue);
    }
}
