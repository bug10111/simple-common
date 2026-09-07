package com.simple.common.rabbitmq.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 本地兜底消息文件写入器（JSON 行格式，按天滚动追加写）
 * <p>
 * 供死信消息与发送失败消息的默认兜底实现使用：每条消息序列化为一行 JSON，
 * 追加写入按天滚动的 {@code {前缀}-yyyyMMdd.jsonl} 文件；写入失败仅记录日志，
 * 不向调用方抛出异常，避免兜底动作反噬消息消费与发送主流程。
 * 该实现仅为本地文件兜底，生产环境建议实现 DeadLetterService / SendFailurePersistenceManager
 * 接口将失败消息落库替换。
 * </p>
 *
 * @author qty
 */
@Slf4j
public class LocalFallbackFileWriter {

    /**
     * 追加写同步锁：同一进程内多线程并发追加时不产生交叉行
     */
    private final Object writeLock = new Object();

    /**
     * 兜底文件根目录（来自 RabbitMqProperties.fallbackDir）
     */
    private final String baseDir;

    /**
     * 兜底文件名前缀（如 dead-letter、send-failure）
     */
    private final String filePrefix;

    /**
     * 构造兜底写入器
     *
     * @param baseDir    兜底文件根目录
     * @param filePrefix 兜底文件名前缀
     */
    public LocalFallbackFileWriter(String baseDir, String filePrefix) {
        this.baseDir = baseDir;
        this.filePrefix = filePrefix;
    }

    /**
     * 追加一行 JSON 兜底记录到当日文件
     *
     * @param jsonLine 单条记录的 JSON 文本（不含换行符）
     */
    public void append(String jsonLine) {
        synchronized (writeLock) {
            try {
                Path dir = Paths.get(baseDir);
                // 目录不存在时按层级创建，保证兜底目录首次使用即可写
                Files.createDirectories(dir);
                // 按天滚动：每天一个独立文件，避免单文件无限增长
                Path file = dir.resolve(filePrefix + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".jsonl");
                Files.write(file, (jsonLine + System.lineSeparator()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) {
                // 兜底写入失败仅记录日志不抛出，保证不影响消息消费与发送主流程
                log.error("本地兜底消息写入失败，baseDir={}, filePrefix={}", baseDir, filePrefix, e);
            }
        }
    }
}
