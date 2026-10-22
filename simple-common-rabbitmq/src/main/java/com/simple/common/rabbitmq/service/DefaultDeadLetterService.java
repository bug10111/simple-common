package com.simple.common.rabbitmq.service;

import com.simple.common.core.utils.JsonUtils;
import com.simple.common.rabbitmq.common.properties.RabbitMqProperties;
import com.simple.common.rabbitmq.common.service.DeadLetterService;
import com.simple.common.rabbitmq.common.utils.LocalFallbackFileWriter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 死信队列服务默认实现：死信消息本地文件兜底持久化（JSON 行格式，按天滚动追加写）
 * <p>
 * 重试耗尽的死信消息写入本地兜底文件（目录取 RabbitMqProperties.fallbackDir），避免未替换时被静默丢弃；
 * 写入结果以 {@link LocalFallbackFileWriter#append} 返回值为准区分成败日志，兜底动作不反噬消费流程。
 * 该实现仅为本地文件兜底，生产环境建议实现 {@link DeadLetterService} 接口将死信落库替换。
 * 兜底文件可能包含敏感信息（凭据/手机号等），文件权限已按属主收紧；
 * 如有合规脱敏要求，请实现自定义 DeadLetterService 在落库前完成脱敏。
 * </p>
 *
 * @author qty
 */
@Slf4j
public class DefaultDeadLetterService implements DeadLetterService {

    /**
     * 死信兜底文件名前缀
     */
    private static final String DEAD_LETTER_FILE_PREFIX = "dead-letter";

    /**
     * 死信兜底记录类型标识
     */
    private static final String DEAD_LETTER_RECORD_TYPE = "DEAD_LETTER";

    private final RabbitMqProperties rabbitMqProperties;

    /**
     * 本地兜底文件写入器（目录可配置，文件按天滚动，自动清理过期文件）
     */
    private final LocalFallbackFileWriter fallbackWriter;

    /**
     * 构造服务并基于配置目录创建兜底写入器
     *
     * @param rabbitMqProperties RabbitMQ 配置属性
     */
    public DefaultDeadLetterService(RabbitMqProperties rabbitMqProperties) {
        this.rabbitMqProperties = rabbitMqProperties;
        this.fallbackWriter = new LocalFallbackFileWriter(rabbitMqProperties.getFallbackDir(),
                DEAD_LETTER_FILE_PREFIX, rabbitMqProperties.getFallbackRetentionDays());
    }

    @Override
    public void save(String exchange, String key, String queue, String body, Exception e) {
        try {
            // 写入本地兜底文件，防止重试耗尽的死信消息被静默丢弃；按返回值区分成败，不再无条件宣称成功
            boolean written = fallbackWriter.append(buildRecord(exchange, key, queue, body, e));
            if (written) {
                log.error("死信消息已写入本地兜底文件（目录[{}]），生产环境请实现接口[DeadLetterService]落库保存！exchange[{}]=>key[{}]=>queue[{}]",
                        rabbitMqProperties.getFallbackDir(), exchange, key, queue);
            } else {
                log.error("死信消息写入本地兜底文件失败（记录已丢弃，内容见写入器错误日志），exchange[{}]=>key[{}]=>queue[{}]", exchange, key, queue);
            }
        } catch (Exception ex) {
            // 记录组装/写入异常仅记日志，不反噬消费流程（调用方在消费异常处理路径上，二次异常会掩盖根因）
            log.error("死信兜底记录组装/写入失败，exchange[{}]=>key[{}]=>queue[{}]", exchange, key, queue, ex);
        }
    }

    /**
     * 组装死信兜底记录（单行 JSON）
     *
     * @param exchange 交换机
     * @param key      路由键
     * @param queue    队列
     * @param body     消息内容
     * @param e        异常
     * @return JSON 格式兜底记录
     */
    private String buildRecord(String exchange, String key, String queue, String body, Exception e) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        record.put("type", DEAD_LETTER_RECORD_TYPE);
        record.put("exchange", exchange);
        record.put("routingKey", key);
        record.put("queue", queue);
        record.put("body", body);
        record.put("exception", e == null ? null : e.getClass().getName() + ": " + e.getMessage());
        return JsonUtils.toJsonStr(record);
    }
}
