package com.simple.common.rabbitmq.common.entity;

import org.springframework.amqp.rabbit.connection.CorrelationData;

import java.util.Arrays;
import java.util.Objects;

/**
 * 增强的 CorrelationData，携带原始消息内容、交换机、路由键，
 * 供 ConfirmCallback 在确认失败时获取完整信息。
 * <p>
 * 线程可见性：实例在发送线程组装、在 RabbitMQ 客户端回调线程读取，字段均声明为 volatile 保证可见；
 * {@code messageBody} 字节在写入与读取时均做防御性拷贝，发送发起后载荷不可再被外部篡改，
 * 确认失败回调读到的是发送时的一致快照。
 * </p>
 *
 * @author qty
 */
public class EnhancedCorrelationData extends CorrelationData {

    /**
     * 消息体（DefaultMessage 对象）
     */
    private volatile DefaultMessage defaultMessage;

    /**
     * 交换机名称
     */
    private volatile String exchange;

    /**
     * 路由键
     */
    private volatile String routingKey;

    /**
     * 原始消息的字节数组（用于无法反序列化时的兜底）
     */
    private volatile byte[] messageBody;

    /**
     * 创建实例并校验必填参数
     *
     * @param id 消息唯一ID，不允许为空白
     */
    public EnhancedCorrelationData(String id) {
        super(Objects.requireNonNull(id, "correlationId 不能为空"));
        if (id.isBlank()) {
            throw new IllegalArgumentException("correlationId 不能为空白");
        }
    }

    /**
     * 创建 EnhancedCorrelationData 实例并校验必填参数
     * <p>
     * 注意：messageBody 为可选兜底载荷；DefaultRabbitMqService 的常规发送路径会传入 null，
     * 并在消息后置处理器中回填实际消息体字节。
     * </p>
     *
     * @param id             消息唯一ID（必填，非空白）
     * @param defaultMessage 消息体
     * @param exchange       交换机
     * @param routingKey     路由键
     * @param messageBody    原始字节（可为 null）
     * @return EnhancedCorrelationData 实例
     */
    public static EnhancedCorrelationData of(String id, DefaultMessage defaultMessage,
                                             String exchange, String routingKey, byte[] messageBody) {
        EnhancedCorrelationData data = new EnhancedCorrelationData(id);
        data.defaultMessage = defaultMessage;
        data.exchange = Objects.requireNonNull(exchange, "exchange 不能为空");
        data.routingKey = Objects.requireNonNull(routingKey, "routingKey 不能为空");
        data.messageBody = messageBody == null ? null : Arrays.copyOf(messageBody, messageBody.length);
        return data;
    }

    public DefaultMessage getDefaultMessage() {
        return defaultMessage;
    }

    public String getExchange() {
        return exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    /**
     * 获取消息体字节的防御性拷贝，避免确认回调读取到被外部篡改的数组
     */
    public byte[] getMessageBody() {
        return messageBody == null ? null : Arrays.copyOf(messageBody, messageBody.length);
    }

    /**
     * 存储消息体字节（存储防御性拷贝）
     */
    public void setMessageBody(byte[] messageBody) {
        this.messageBody = messageBody == null ? null : Arrays.copyOf(messageBody, messageBody.length);
    }
}
