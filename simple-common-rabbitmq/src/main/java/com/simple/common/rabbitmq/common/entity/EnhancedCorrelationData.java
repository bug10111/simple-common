package com.simple.common.rabbitmq.common.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.amqp.rabbit.connection.CorrelationData;

/**
 * 增强的 CorrelationData，携带原始消息内容、交换机、路由键，
 * 供 ConfirmCallback 在确认失败时获取完整信息。
 *
 * @author qty
 */
@Getter
@Setter
public class EnhancedCorrelationData extends CorrelationData {

    /**
     * 消息体（DefaultMessage 对象）
     */
    private DefaultMessage defaultMessage;

    /**
     * 交换机名称
     */
    private String exchange;

    /**
     * 路由键
     */
    private String routingKey;

    /**
     * 原始消息的字节数组（用于无法反序列化时的兜底）
     */
    private byte[] messageBody;

    public EnhancedCorrelationData(String id) {
        super(id);
    }

    /**
     * 创建 EnhancedCorrelationData 实例
     *
     * @param id             消息唯一ID
     * @param defaultMessage 消息体
     * @param exchange       交换机
     * @param routingKey     路由键
     * @param messageBody    原始字节
     * @return EnhancedCorrelationData 实例
     */
    public static EnhancedCorrelationData of(String id, DefaultMessage defaultMessage,
                                             String exchange, String routingKey, byte[] messageBody) {
        EnhancedCorrelationData data = new EnhancedCorrelationData(id);
        data.setDefaultMessage(defaultMessage);
        data.setExchange(exchange);
        data.setRoutingKey(routingKey);
        data.setMessageBody(messageBody);
        return data;
    }
}