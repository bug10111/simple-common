package com.simple.common.rabbitmq.manager;

import com.simple.common.core.utils.JsonUtils;
import com.simple.common.rabbitmq.common.entity.DefaultMessage;
import com.simple.common.rabbitmq.common.manager.SendFailurePersistenceManager;
import com.simple.common.rabbitmq.common.properties.RabbitMqProperties;
import com.simple.common.rabbitmq.common.utils.LocalFallbackFileWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 发送失败持久化默认实现：发布确认失败/路由失败消息本地文件兜底持久化（JSON 行格式，按天滚动追加写）
 * <p>
 * 失败消息写入本地兜底文件（目录取 RabbitMqProperties.fallbackDir），避免未替换时发送失败消息丢失；
 * 兜底写入失败仅记录日志，不反噬发送流程。该实现仅为本地文件兜底，
 * 生产环境建议实现 {@link SendFailurePersistenceManager} 接口将失败消息落库并补偿重试。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultSendFailurePersistenceManager implements SendFailurePersistenceManager {

    /**
     * 发送失败兜底文件名前缀
     */
    private static final String SEND_FAILURE_FILE_PREFIX = "send-failure";

    /**
     * 发布确认失败兜底记录类型标识
     */
    private static final String RECORD_TYPE_CONFIRM_FAILURE = "SEND_CONFIRM_FAILURE";

    /**
     * 路由失败兜底记录类型标识
     */
    private static final String RECORD_TYPE_RETURN_FAILURE = "SEND_RETURN_FAILURE";

    /**
     * 本地兜底文件写入器（目录可配置，文件按天滚动）
     */
    private final LocalFallbackFileWriter fallbackWriter;

    /**
     * 构造管理器并基于配置目录创建兜底写入器
     *
     * @param rabbitMqProperties RabbitMQ 配置属性
     */
    public DefaultSendFailurePersistenceManager(RabbitMqProperties rabbitMqProperties) {
        this.fallbackWriter = new LocalFallbackFileWriter(rabbitMqProperties.getFallbackDir(), SEND_FAILURE_FILE_PREFIX);
    }

    @Override
    public void saveConfirmFailure(String correlationId, String cause, String receivedExchange, String receivedRoutingKey, DefaultMessage defaultMessage, byte[] messageBody) {
        // 消息体优先取反序列化对象，其次取原始字节，均无时记录占位说明
        String bodyStr = defaultMessage != null ? JsonUtils.toJsonStr(defaultMessage) : (messageBody != null ? new String(messageBody, StandardCharsets.UTF_8) : null);
        fallbackWriter.append(buildRecord(RECORD_TYPE_CONFIRM_FAILURE, correlationId, cause, receivedExchange, receivedRoutingKey, null, null, bodyStr));
        log.error("发送交换机消息失败已写入本地兜底文件，correlationId={}, exchange={}, routingKey={}，生产环境请实现 SendFailurePersistenceManager 接口落库保存！", correlationId, receivedExchange, receivedRoutingKey);
    }

    @Override
    public void saveReturnFailure(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        String bodyStr = message != null && message.getBody() != null ? new String(message.getBody(), StandardCharsets.UTF_8) : null;
        String correlationId = message != null ? message.getMessageProperties().getCorrelationId() : null;
        // 路由失败的 cause 信息由 replyCode + replyText 完整表达，不再重复记录
        fallbackWriter.append(buildRecord(RECORD_TYPE_RETURN_FAILURE, correlationId, null, exchange, routingKey, replyCode, replyText, bodyStr));
        log.error("消息路由失败已写入本地兜底文件，exchange={}, routingKey={}, replyCode={}, replyText={}，生产环境请实现 SendFailurePersistenceManager 接口落库保存！", exchange, routingKey, replyCode, replyText);
    }

    /**
     * 组装发送失败兜底记录（单行 JSON）
     *
     * @param type        记录类型标识
     * @param correlationId 消息唯一ID
     * @param cause       失败原因
     * @param exchange    交换机
     * @param routingKey  路由键
     * @param replyCode   路由失败回复码（发布确认失败场景为 null）
     * @param replyText   路由失败回复文本（发布确认失败场景为 null）
     * @param body        消息体文本
     * @return JSON 格式兜底记录
     */
    private String buildRecord(String type, String correlationId, String cause, String exchange, String routingKey, Integer replyCode, String replyText, String body) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        record.put("type", type);
        record.put("correlationId", correlationId);
        record.put("cause", cause);
        record.put("exchange", exchange);
        record.put("routingKey", routingKey);
        record.put("replyCode", replyCode);
        record.put("replyText", replyText);
        record.put("body", body);
        return JsonUtils.toJsonStr(record);
    }
}
