package com.simple.common.rabbitmq.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地兜底消息文件写入器（JSON 行格式，按天滚动追加写）
 * <p>
 * 供死信消息与发送失败消息的默认兜底实现使用：每条消息序列化为一行 JSON，
 * 追加写入按天滚动的 {@code {前缀}-yyyyMMdd.jsonl} 文件；写入失败仅记录日志并返回 false，
 * 不向调用方抛出异常，避免兜底动作反噬消息消费与发送主流程（调用方应按返回值区分成败日志）。
 * </p>
 * <p>
 * 并发说明：锁按目标文件路径（baseDir+前缀）全局分配，同一 JVM 内多个写入器实例写同一文件时互斥；
 * 跨进程并发由文件追加原子性+按行写入保证（极端截断风险留痕于错误日志）。
 * 目录权限：POSIX 文件系统下按属主可读写收紧（内容可能含敏感信息）。
 * </p>
 *
 * @author qty
 */
@Slf4j
public class LocalFallbackFileWriter {

    /**
     * 按兜底文件路径（目录+前缀）加锁，保证不同写入器实例写同一文件时不交叉
     */
    private static final Map<String, Object> FILE_LOCKS = new ConcurrentHashMap<>();

    /**
     * 滚动文件日期格式（静态常量，避免每次追加重复构建）
     */
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    /**
     * 兜底文件根目录（来自 RabbitMqProperties.fallbackDir，构造时一次解析）
     */
    private final Path baseDir;

    /**
     * 兜底文件名前缀（如 dead-letter、send-failure，构造时校验合法性）
     */
    private final String filePrefix;

    /**
     * 兜底文件保留天数（0 表示不自动清理）
     */
    private final int retentionDays;

    /**
     * 最近一次过期文件清理日期（每日至多清理一次，volatile 保证跨线程可见）
     */
    private volatile LocalDate lastRetentionCheck;

    /**
     * 构造兜底写入器
     *
     * @param baseDir       兜底文件根目录（不允许为空白）
     * @param filePrefix    兜底文件名前缀（不允许为空白，不允许包含路径分隔符或 ".."，防止路径穿越）
     * @param retentionDays 兜底文件保留天数（小于 0 按 0 处理）
     */
    public LocalFallbackFileWriter(String baseDir, String filePrefix, int retentionDays) {
        if (baseDir == null || baseDir.isBlank()) {
            throw new IllegalArgumentException("兜底目录不允许为空白");
        }
        validateFilePrefix(filePrefix);
        this.baseDir = Path.of(baseDir).toAbsolutePath().normalize();
        this.filePrefix = filePrefix.trim();
        this.retentionDays = Math.max(0, retentionDays);
    }

    /**
     * 追加一行 JSON 兜底记录到当日文件
     *
     * @param jsonLine 单条记录的 JSON 文本（不含换行符）
     * @return true-写入成功；false-写入失败或入参为空（记录被丢弃，仅记录错误日志）
     */
    public boolean append(String jsonLine) {
        if (jsonLine == null || jsonLine.isEmpty()) {
            log.warn("本地兜底消息内容为空，跳过写入，baseDir={}, filePrefix={}", baseDir, filePrefix);
            return false;
        }
        LocalDate today = LocalDate.now();
        Path file = baseDir.resolve(filePrefix + "-" + today.format(DAY_FORMATTER) + ".jsonl");
        // 锁按文件路径全局分配，避免多实例交叉写同一文件
        Object fileLock = FILE_LOCKS.computeIfAbsent(file.toString(), key -> new Object());
        synchronized (fileLock) {
            try {
                // 目录缺失时按层级创建（仅在缺失时触发系统调用）
                if (!Files.isDirectory(baseDir)) {
                    Files.createDirectories(baseDir);
                    tightenPermissions(baseDir);
                }
                Files.write(file, (jsonLine + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                if (Files.notExists(file) == false) {
                    tightenPermissions(file);
                }
            } catch (Exception e) {
                // 兜底写入失败仅记录日志不抛出（含被丢弃内容摘要），保证不影响消息消费与发送主流程
                log.error("本地兜底消息写入失败，记录将被丢弃，baseDir={}, filePrefix={}, recordLength={}, record={}",
                        baseDir, filePrefix, jsonLine.length(), abbreviate(jsonLine), e);
                return false;
            }
            sweepRetentionIfNeeded(today);
            return true;
        }
    }

    /**
     * 按天至多一次的过期兜底文件清理，防止文件无限累积写满磁盘
     */
    private void sweepRetentionIfNeeded(LocalDate today) {
        if (retentionDays <= 0) {
            return;
        }
        LocalDate lastCheck = lastRetentionCheck;
        if (today.equals(lastCheck)) {
            return;
        }
        lastRetentionCheck = today;
        LocalDate expireBefore = today.minusDays(retentionDays);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir, filePrefix + "-*.jsonl")) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                String dayPart = fileName.substring(filePrefix.length() + 1, fileName.length() - ".jsonl".length());
                LocalDate fileDay = LocalDate.parse(dayPart, DAY_FORMATTER);
                if (fileDay.isBefore(expireBefore)) {
                    Files.deleteIfExists(file);
                    log.info("清理过期本地兜底文件：{}（保留天数={}）", file, retentionDays);
                }
            }
        } catch (Exception e) {
            // 清理失败不影响主流程，等待下次触发
            log.warn("本地兜底文件清理失败，baseDir={}, filePrefix={}", baseDir, filePrefix, e);
        }
    }

    /**
     * POSIX 文件系统下收紧目录/文件权限为仅属主可读写（Windows 等不支持 POSIX 的文件系统静默跳过）
     */
    private void tightenPermissions(Path path) {
        try {
            java.nio.file.attribute.PosixFileAttributeView attributeView = Files.getFileAttributeView(path, java.nio.file.attribute.PosixFileAttributeView.class);
            if (attributeView != null) {
                attributeView.setPermissions(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            }
        } catch (UnsupportedOperationException | IOException ignore) {
            // 非 POSIX 文件系统或权限设置失败时忽略，交由部署层目录权限兜底
        } catch (Exception e) {
            log.debug("兜底文件权限收紧失败：{}", path, e);
        }
    }

    /**
     * 文件名前缀合法性校验：非空白、不含路径分隔符与 ".."，防止配置值穿越兜底目录
     */
    private void validateFilePrefix(String filePrefix) {
        if (filePrefix == null || filePrefix.isBlank()) {
            throw new IllegalArgumentException("兜底文件名前缀不允许为空白");
        }
        if (filePrefix.contains("/") || filePrefix.contains("\\") || filePrefix.contains("..")) {
            throw new IllegalArgumentException("兜底文件名前缀不允许包含路径分隔符或 '..': " + filePrefix);
        }
    }

    /**
     * 错误日志中输出内容摘要（截断到 2048 字符），便于人工找回被丢弃记录
     */
    private static String abbreviate(String value) {
        return value.length() <= 2048 ? value : value.substring(0, 2048) + "...(截断)";
    }
}
