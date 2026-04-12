package com.simple.common.logs.server.manager;

import cn.hutool.json.JSONUtil;
import com.simple.common.core.utils.ThreadUtils;
import com.simple.common.logs.proto.common.event.LogDataEvent;
import com.simple.common.logs.server.common.manager.LogsSaveManager;
import com.simple.common.logs.server.common.properties.LogTcpServerProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 抽象日志保存管理器，提供内存队列 + 批量处理 + WAL 兜底能力 + 死信队列
 * <p>
 * 子类只需实现 {@link #persistence(List)} 方法，定义具体的持久化逻辑（如 ES、向量库等）。
 * 本类负责队列管理、批量调度、异常降级、WAL 恢复和死信处理。
 * </p>
 *
 * <h3>⚠️ 重要：幂等性要求</h3>
 * <p>
 * 由于 WAL 恢复机制可能在进程崩溃后重放已成功持久化的日志，子类实现的 {@link #persistence(List)}
 * 方法<b>必须保证幂等性</b>。即：同一批日志被重复调用时，不应产生重复数据。
 * </p>
 * <p>
 * 实现幂等的常见方式：
 * <ul>
 *     <li>使用数据库唯一键（如 traceId + createTimestamp）进行 INSERT IGNORE 或 ON DUPLICATE KEY UPDATE。</li>
 *     <li>在写入文件前检查目标文件中是否已包含相同 traceId。</li>
 *     <li>使用外部去重存储（如 Redis Set）记录已处理的 traceId。</li>
 * </ul>
 * 若无法保证幂等，建议关闭 WAL 恢复功能（walRecoveryEnabled = false），仅依靠人工介入处理 WAL 文件。
 * </p>
 *
 * @author qty
 */
@Slf4j
public abstract class AbsLogsSaveManager implements LogsSaveManager, InitializingBean {

    protected abstract LogTcpServerProperties getProperties();

    /**
     * 日志数据队列
     */
    private final LinkedBlockingQueue<LogDataEvent> logQueue = new LinkedBlockingQueue<>(50000);

    /**
     * WAL 写入锁
     */
    private final ReentrantLock walLock = new ReentrantLock();

    /**
     * WAL 文件写入器
     */
    private BufferedWriter walWriter;

    /**
     * 当前 WAL 文件
     */
    private File currentWalFile;

    /**
     * 是否正在运行（用于控制后台线程）
     */
    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * WAL 恢复线程
     */
    private Thread walRecoveryThread;

    @Override
    public void saveLog(LogDataEvent logDataEvent) {
        try {
            boolean success = logQueue.offer(logDataEvent);
            if (!success) {
                log.warn("日志队列已满，丢弃日志: {}", logDataEvent.getTraceId());
                // 写 WAL 兜底
                writeWAL(logDataEvent);
            } else {
                log.debug("添加日志到队列，当前队列大小: {}", logQueue.size());
            }
        } catch (Exception e) {
            log.error("添加日志到队列失败", e);
        }
    }

    /**
     * 批量保存日志
     * <p>
     * 当前实现为循环调用 {@link #saveLog(LogDataEvent)}，每条日志独立入队。
     * 之所以未采用"将整个批次作为一个元素入队"的优化，是基于以下权衡：
     * <ul>
     *     <li>1. 改动成本：修改队列元素类型会影响消费端 {@link #processLogs()} 逻辑，需引入扁平化处理。</li>
     *     <li>2. 锁开销可接受：{@link LinkedBlockingQueue#offer(Object)} 内部为短临界区，
     *         在典型批次大小（100~1000）下，锁竞争开销远小于网络/磁盘 IO。</li>
     *     <li>3. 故障隔离：逐条入队允许部分成功，队列满时仅将超出部分降级 WAL，而非整个批次失败。</li>
     * </ul>
     * 若未来需极致优化，可考虑将队列改为 {@code LinkedBlockingQueue<List<LogDataEvent>>}
     * 并在消费端使用 {@code drainTo} 批量获取批次列表，再扁平化处理。
     * </p>
     *
     * @param events 日志数据事件列表
     */
    @Override
    public void saveLogBatch(List<LogDataEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (LogDataEvent event : events) {
            saveLog(event);
        }
    }

    @Override
    public void processLogs() {
        List<LogDataEvent> logsToSave = new ArrayList<>();
        logQueue.drainTo(logsToSave, getProperties().getBatchSize());
        if (!logsToSave.isEmpty()) {
            try {
                persistence(logsToSave);
            } catch (Exception e) {
                log.error("持久化失败，将写入 WAL", e);
                // 持久化失败，写 WAL 兜底
                for (LogDataEvent event : logsToSave) {
                    writeWAL(event);
                }
            }
        }
    }

    /**
     * 写入预写日志（WAL）
     * <p>
     * 当队列满或持久化失败时，将日志写入本地文件作为最后兜底。
     * </p>
     *
     * @param event 日志事件
     */
    private void writeWAL(LogDataEvent event) {
        if (!getProperties().isWalEnabled()) {
            return;
        }
        walLock.lock();
        try {
            if (walWriter == null) {
                initWalWriter();
            }
            String json = JSONUtil.toJsonStr(event);
            walWriter.write(json);
            walWriter.newLine();
            walWriter.flush();

            // 检查文件大小
            if (currentWalFile.length() > getProperties().getWalFileMaxSize()) {
                rotateWalFile();
            }
        } catch (IOException e) {
            log.error("写入 WAL 失败", e);
        } finally {
            walLock.unlock();
        }
    }

    private void initWalWriter() throws IOException {
        Path dir = Paths.get(getProperties().getWalDir());
        Files.createDirectories(dir);
        currentWalFile = dir.resolve("wal.log").toFile();
        walWriter = new BufferedWriter(new FileWriter(currentWalFile, true));
        log.info("WAL 写入器初始化完成: {}", currentWalFile.getAbsolutePath());
    }

    private void rotateWalFile() throws IOException {
        walWriter.close();
        Path dir = Paths.get(getProperties().getWalDir());
        String timestamp = String.valueOf(System.currentTimeMillis());
        Path newFile = dir.resolve("wal-" + timestamp + ".log");
        Files.move(currentWalFile.toPath(), newFile, StandardCopyOption.ATOMIC_MOVE);
        currentWalFile = dir.resolve("wal.log").toFile();
        walWriter = new BufferedWriter(new FileWriter(currentWalFile, true));
        log.info("WAL 文件已轮转: {}", newFile.getFileName());
    }

    @Override
    public int getQueueSize() {
        return logQueue.size();
    }

    /**
     * 持久化抽象方法
     * <p>
     * 由子类实现具体的存储逻辑，例如写入 Elasticsearch、向量数据库或文件。
     * 该方法在定时调度线程中被调用，传入的是当前批次待持久化的日志列表。
     * </p>
     * <p>
     * <b>⚠️ 幂等性要求</b>：此方法可能被 WAL 恢复机制重复调用，必须保证幂等。
     * </p>
     *
     * @param logsToSave 待持久化的日志事件列表
     */
    protected abstract void persistence(List<LogDataEvent> logsToSave);

    @Override
    public void afterPropertiesSet() {
        // 定时批量处理（单位：毫秒）
        ThreadUtils.scheduleWithFixedDelay(() -> {
            try {
                processLogs();
            } catch (Exception e) {
                log.error("任务执行失败: {}", e.getMessage());
            }
        }, getProperties().getBatchInterval(), TimeUnit.MILLISECONDS);

        // 启动 WAL 恢复线程
        if (getProperties().isWalEnabled() && getProperties().isWalRecoveryEnabled()) {
            walRecoveryThread = new Thread(this::walRecoveryLoop, "WAL-Recovery");
            walRecoveryThread.setDaemon(true);
            walRecoveryThread.start();
            log.info("WAL 恢复线程已启动，扫描间隔: {} ms", getProperties().getWalRecoveryInterval());
        }
    }

    /**
     * WAL 恢复循环
     * <p>
     * 定期扫描 WAL 目录下已轮转的文件，尝试将其中的日志重新持久化。
     * </p>
     */
    private void walRecoveryLoop() {
        while (running.get()) {
            try {
                recoverWalFiles();
                Thread.sleep(getProperties().getWalRecoveryInterval());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("WAL 恢复线程被中断");
                break;
            } catch (Exception e) {
                log.error("WAL 恢复循环异常", e);
            }
        }
        log.info("WAL 恢复线程退出");
    }

    /**
     * 扫描并恢复所有已轮转的 WAL 文件
     */
    private void recoverWalFiles() {
        Path dir = Paths.get(getProperties().getWalDir());
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.newDirectoryStream(dir, "wal-*.log")) {
            for (Path file : stream) {
                recoverSingleWalFile(file);
            }
        } catch (IOException e) {
            log.error("扫描 WAL 文件失败", e);
        }
    }

    /**
     * 恢复单个 WAL 文件（流式处理，避免 OOM，支持死信队列）
     * <p>
     * 逐行读取 JSON 日志，分批调用持久化方法。
     * 解析失败的行将被移入死信队列，不阻塞正常日志恢复。
     * 只有当文件内所有有效日志都成功持久化后，才删除该文件。
     * </p>
     *
     * @param file WAL 文件路径
     */
    private void recoverSingleWalFile(Path file) {
        log.info("开始恢复 WAL 文件: {}", file.getFileName());
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            List<LogDataEvent> batch = new ArrayList<>(getProperties().getBatchSize());
            int successCount = 0;
            int totalValidCount = 0;   // 有效日志总数（解析成功）
            int deadCount = 0;         // 死信数量（解析失败）
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                // 修正：将 Path 转换为 String
                LogDataEvent event = parseWalLine(line, file.getFileName().toString(), lineNumber);
                if (event == null) {
                    deadCount++;
                    continue;
                }
                totalValidCount++;
                batch.add(event);
                if (batch.size() >= getProperties().getBatchSize()) {
                    successCount += tryPersistBatch(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                successCount += tryPersistBatch(batch);
            }

            // 全部有效日志成功则删除文件
            if (successCount == totalValidCount && totalValidCount > 0) {
                Files.deleteIfExists(file);
                log.info("WAL 文件恢复完成，已删除: {}, 成功: {}/{}, 死信: {}",
                        file.getFileName(), successCount, totalValidCount, deadCount);
            } else {
                log.warn("WAL 文件部分恢复失败，保留文件: {}, 成功: {}, 有效总数: {}, 死信: {}",
                        file.getFileName(), successCount, totalValidCount, deadCount);
            }
        } catch (IOException e) {
            log.error("读取 WAL 文件失败: {}", file, e);
        }
    }

    /**
     * 解析单行 WAL 日志，失败时写入死信队列
     *
     * @param line       JSON 字符串
     * @param fileName   源文件名（用于日志）
     * @param lineNumber 行号（用于日志）
     * @return 解析成功返回 LogDataEvent，失败返回 null
     */
    private LogDataEvent parseWalLine(String line, String fileName, int lineNumber) {
        try {
            return JSONUtil.toBean(line, LogDataEvent.class);
        } catch (Exception e) {
            log.error("WAL 行解析失败 [{}:{}]: {}", fileName, lineNumber, line, e);
            writeToDeadLetter(line, fileName, lineNumber);
            return null;
        }
    }

    /**
     * 将解析失败或无法恢复的日志写入死信队列
     *
     * @param originalLine 原始 JSON 行
     * @param sourceFile   来源文件名
     * @param lineNumber   行号
     */
    private void writeToDeadLetter(String originalLine, String sourceFile, int lineNumber) {
        if (!getProperties().isDeadLetterEnabled()) {
            return;
        }
        try {
            Path dir = Paths.get(getProperties().getDeadLetterDir());
            Files.createDirectories(dir);
            // 按日期命名死信文件，便于人工排查
            String dateStr = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
            Path deadFile = dir.resolve("deadletter-" + dateStr + ".log");

            // 附加元数据：来源文件、行号、时间戳
            String record = String.format("{\"timestamp\":\"%s\",\"sourceFile\":\"%s\",\"lineNumber\":%d,\"rawData\":%s}",
                    new java.util.Date(), sourceFile, lineNumber, originalLine);

            Files.write(deadFile, (record + System.lineSeparator()).getBytes(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
            log.debug("已将损坏日志写入死信队列: {}", deadFile.getFileName());
        } catch (IOException e) {
            log.error("写入死信队列失败", e);
        }
    }

    /**
     * 尝试持久化一个批次，失败时返回 0
     *
     * @param batch 待持久化的日志事件列表
     * @return 成功持久化的条数
     */
    private int tryPersistBatch(List<LogDataEvent> batch) {
        try {
            persistence(batch);
            return batch.size();
        } catch (Exception e) {
            log.error("恢复 WAL 批次持久化失败，批次大小: {}", batch.size(), e);
            return 0;
        }
    }

    /**
     * 关闭资源，优雅停止后台线程
     * <p>
     * 注意：该方法需要子类在适当的生命周期（如 @PreDestroy）中调用。
     * 由于本类为抽象类且未直接注册为 Bean，子类应覆盖并调用 super.shutdown()。
     * </p>
     */
    @PreDestroy
    public void shutdown() {
        running.set(false);
        if (walRecoveryThread != null) {
            try {
                walRecoveryThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("等待 WAL 恢复线程结束时被中断");
            }
        }
        walLock.lock();
        try {
            if (walWriter != null) {
                walWriter.close();
                log.info("WAL 写入器已关闭");
            }
        } catch (IOException e) {
            log.error("关闭 WAL 写入器失败", e);
        } finally {
            walLock.unlock();
        }
    }
}