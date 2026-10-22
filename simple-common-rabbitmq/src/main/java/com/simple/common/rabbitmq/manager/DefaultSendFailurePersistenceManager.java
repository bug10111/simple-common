package com.simple.common.rabbitmq.manager;

import com.simple.common.core.utils.JsonUtils;
import com.simple.common.rabbitmq.common.entity.DefaultMessage;
import com.simple.common.rabbitmq.common.manager.SendFailurePersistenceManager;
import com.simple.common.rabbitmq.common.properties.RabbitMqProperties;
import com.simple.common.rabbitmq.common.utils.LocalFallbackFileWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 发送失败持久化默认实现：发布确认失败/路由失败消息本地文件兜底持久化（JSON 行格式，按天滚动追加写）
 * <p>
 * 失败消息写入本地兜底文件（目录取 RabbitMqProperties.fallbackDir），避免未替换时发送失败消息丢失；
 * 写入结果以 {@link LocalFallbackFileWriter#append} 返回值为准区分成败日志，兜底动作不反噬发送流程。
 * 该实现仅为本地文件兜底，生产环境建议实现 {@link SendFailurePersistenceManager} 接口将失败消息落库并补偿重试。
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
    private static final String CONFIRM_FAILURE_RECORD_TYPE = "SEND_CONFIRM_FAILURE";

    /**
     * 路由失败兜底记录类型标识
     */
    private static final String RETURN_FAILURE_RECORD_TYPE = "SEND_RETURN_FAILURE";

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
        this.fallbackWriter = new LocalFallbackFileWriter(rabbitMqProperties.getFallbackDir(),
                SEND_FAILURE_FILE_PREFIX, rabbitMqProperties.getFallbackRetentionDays());
    }

    @Override
    public void saveConfirmFailure(String correlationId, String cause, String receivedExchange, String receivedRoutingKey, DefaultMessage defaultMessage, byte[] messageBody) {
        // 消息体优先取反序列化对象，其次取原始字节，均为空时记录占位说明
        String bodyStr = defaultMessage != null ? JsonUtils.toJsonStr(defaultMessage)
                : toReadableBody(messageBody);
        try {
            boolean written = fallbackWriter.append(buildRecord(CONFIRM_FAILURE_RECORD_TYPE, correlationId, cause, receivedExchange, receivedRoutingKey, null, null, bodyStr));
            if (written) {
                log.error("发送确认失败已写入本地兜底文件，correlationId={}, exchange={}, routingKey={}，生产环境请实现 SendFailurePersistenceManager 接口落库保存！",
                        correlationId, receivedExchange, receivedRoutingKey);
            } else {
                log.error("发送确认失败写入本地兜底文件失败（记录已丢弃，内容见写入器错误日志），correlationId={}, exchange={}, routingKey={}",
                        correlationId, receivedExchange, receivedRoutingKey);
            }
        } catch (Exception ex) {
            // 记录组装/写入异常仅记日志，不反噬发布确认回调流程
            log.error("发送确认失败兜底记录组装/写入异常，correlationId={}, exchange={}, routingKey={}",
                    correlationId, receivedExchange, receivedRoutingKey, ex);
        }
    }

    @Override
    public void saveReturnFailure(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        String bodyStr = message != null ? toReadableBody(message.getBody()) : "(空消息体)";
        String correlationId = message != null ? message.getMessageProperties().getCorrelationId() : null;
        // 路由失败的 cause 信息由 replyCode + replyText 完整表达，不再重复记录
        try {
            boolean written = fallbackWriter.append(buildRecord(RETURN_FAILURE_RECORD_TYPE, correlationId, null, exchange, routingKey, replyCode, replyText, bodyStr));
            if (written) {
                log.error("消息路由失败已写入本地兜底文件，exchange={}, routingKey={}, replyCode={}, replyText={}，生产环境请实现 SendFailurePersistenceManager 接口落库保存！",
                        exchange, routingKey, replyCode, replyText);
            } else {
                log.error("消息路由失败写入本地兜底文件失败（记录已丢弃，内容见写入器错误日志），exchange={}, routingKey={}, replyCode={}, replyText={}",
                        exchange, routingKey, replyCode, replyText);
            }
        } catch (Exception ex) {
            // 记录组装/写入异常仅记日志，不反噬 ReturnCallback 流程
            log.error("路由失败兜底记录组装/写入异常，exchange={}, routingKey={}", exchange, routingKey, ex);
        }
    }

    /**
     * 组装发送失败兜底记录（单行 JSON）
     *
     * @param type          记录类型标识
     * @param correlationId 消息唯一ID
     * @param cause         失败原因
     * @param exchange      交换机
     * @param routingKey    路由键
     * @param replyCode     路由失败回复码（发布确认失败场景为 null）
     * @param replyText     路由失败回复文本（发布确认失败场景为 null）
     * @param body          消息体文本
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

    /**
     * 消息体无损可读化：UTF-8 可解码时按文本记录；二进制/非 UTF-8 报文转 Base64 并显式标注，
     * 保证兜底记录可无损还原重放（直接 new String 会把非 UTF-8 字节不可逆转成乱码）。
     *
     * @param body 原始消息字节（可为 null）
     * @return 可读文本（UTF-8 文本或 Base64 标注）
     */
    private String toReadableBody(byte[] body) {
        if (body == null || body.length == 0) {
            return "(空消息体)";
        }
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            return decoder.decode(ByteBuffer.wrap(body)).toString();
        } catch (CharacterCodingException notUtf8) {
            return "(非UTF-8二进制消息体，Base64编码=" + Base64.getEncoder().encodeToString(body) + ")";
        }
    }
}
