package com.simple.common.logs.server.manager;

import cn.hutool.json.JSONUtil;
import com.simple.common.core.utils.ThreadUtils;
import com.simple.common.logs.proto.common.event.LogDataEvent;
import com.simple.common.logs.server.common.manager.LogsSaveManager;
import com.simple.common.logs.server.common.properties.LogTcpServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 抽象日志保存管理器，提供内存队列 + 批量处理 + WAL 兜底能力 + 死信队列
 * <p>
 * 子类只需实现 {@link #persistence(List)} 方法，定义具体的持久化逻辑（如 ES、向量库等）。
 * 本类负责队列管理、批量调度、异常降级、WAL 恢复和死信处理。
 * </p>
 * <h3>⚠️ 重要：幂等性要求</h3>
 * <p>
 * 由于 WAL 恢复机制可能在进程崩溃后重放已成功持久化的日志，子类实现的 {@link #persistence(List)}
 * 方法<b>必须保证幂等性</b>。即：同一批日志被重复调用时，不应产生重复数据。
 * </p>
 * <h3>性能优化说明</h3>
 * <p>
 * - 队列元素改为 {@code List<LogDataEvent>}，支持批次入队，大幅减少锁竞争。
 * - WAL 写入改为批量 flush，减少磁盘 I/O 压力。
 * </p>
 *
 * @author qty
 */
@Slf4j
public abstract class AbsLogsSaveManager implements LogsSaveManager, InitializingBean, DisposableBean {

    protected abstract LogTcpServerProperties getProperties();

    /**
     * 日志数据队列，元素为日志批次列表
     * 使用 volatile 保证多线程下的可见性
     */
    private volatile LinkedBlockingQueue<List<LogDataEvent>> logQueue;

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
     * WAL 批量写入计数器
     */
    private int walBatchCount = 0;

    /**
     * 上次 WAL flush 时间
     */
    private long lastWalFlushTime = System.currentTimeMillis();

    /**
     * 是否正在运行（用于控制后台线程）
     */
    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * 定时批量处理任务的 Future（用于关闭时取消）
     */
    private ScheduledFuture<?> processLogsFuture;

    /**
     * WAL 恢复定时任务的 Future（用于关闭时取消）
     */
    private ScheduledFuture<?> walRecoveryFuture;

    /**
     * WAL 恢复任务是否正在执行（防止并发）
     */
    private final AtomicBoolean walRecoveryRunning = new AtomicBoolean(false);

    /**
     * 获取队列实例（延迟初始化）
     */
    private LinkedBlockingQueue<List<LogDataEvent>> getLogQueue() {
        if (logQueue == null) {
            synchronized (this) {
                if (logQueue == null) {
                    logQueue = new LinkedBlockingQueue<>(getProperties().getQueueCapacity());
                }
            }
        }
        return logQueue;
    }

    @Override
    public void saveLog(LogDataEvent logDataEvent) {
        // 单条日志包装为批次入队
        List<LogDataEvent> batch = new ArrayList<>(1);
        batch.add(logDataEvent);
        saveLogBatch(batch);
    }

    /**
     * 批量保存日志
     * <p>
     * 将整个批次作为一个元素入队，消费端扁平化处理，显著减少队列操作次数和锁竞争。
     * </p>
     *
     * @param events 日志数据事件列表
     */
    @Override
    public void saveLogBatch(List<LogDataEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        try {
            boolean success = getLogQueue().offer(events);
            if (!success) {
                log.warn("日志队列已满，批次大小: {}, 将写入 WAL", events.size());
                // 写 WAL 兜底
                writeWALBatch(events);
            } else {
                log.debug("批次日志入队成功，大小: {}, 当前队列大小: {}", events.size(), getLogQueue().size());
            }
        } catch (Exception e) {
            log.error("添加日志批次到队列失败", e);
        }
    }

    @Override
    public void processLogs() {
        // 从队列中批量取出批次列表
        List<List<LogDataEvent>> batches = new ArrayList<>();
        getLogQueue().drainTo(batches, getProperties().getBatchSize());
        if (batches.isEmpty()) {
            return;
        }

        // 扁平化所有批次为单个待持久化列表
        List<LogDataEvent> logsToSave = new ArrayList<>();
        for (List<LogDataEvent> batch : batches) {
            logsToSave.addAll(batch);
        }

        try {
            persistence(logsToSave);
        } catch (Exception e) {
            log.error("持久化失败，将写入 WAL", e);
            // 持久化失败，写 WAL 兜底（将原始批次重新写入）
            for (List<LogDataEvent> batch : batches) {
                writeWALBatch(batch);
            }
        }
    }

    /**
     * 批量写入预写日志（WAL）
     * <p>
     * 将批次内所有日志写入 WAL，但不立即 flush，而是累积到一定数量或时间后统一 flush。
     * </p>
     *
     * @param events 日志事件列表
     */
    private void writeWALBatch(List<LogDataEvent> events) {
        if (!getProperties().isWalEnabled() || events.isEmpty()) {
            return;
        }
        walLock.lock();
        try {
            if (walWriter == null) {
                initWalWriter();
            }
            for (LogDataEvent event : events) {
                String json = JSONUtil.toJsonStr(event);
                walWriter.write(json);
                walWriter.newLine();
            }
            walBatchCount += events.size();

            // 达到批量阈值或距离上次 flush 超过 1 秒，则执行 flush
            boolean shouldFlush = walBatchCount >= 100 || (System.currentTimeMillis() - lastWalFlushTime) > 1000;
            if (shouldFlush) {
                walWriter.flush();
                walBatchCount = 0;
                lastWalFlushTime = System.currentTimeMillis();
            }

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
        lastWalFlushTime = System.currentTimeMillis();
        walBatchCount = 0;
        log.info("WAL 写入器初始化完成: {}", currentWalFile.getAbsolutePath());
    }

    private void rotateWalFile() throws IOException {
        // 强制 flush 剩余数据
        if (walWriter != null) {
            walWriter.flush();
            walWriter.close();
        }
        Path dir = Paths.get(getProperties().getWalDir());
        String timestamp = String.valueOf(System.currentTimeMillis());
        Path newFile = dir.resolve("wal-" + timestamp + ".log");
        Files.move(currentWalFile.toPath(), newFile, StandardCopyOption.ATOMIC_MOVE);
        currentWalFile = dir.resolve("wal.log").toFile();
        walWriter = new BufferedWriter(new FileWriter(currentWalFile, true));
        lastWalFlushTime = System.currentTimeMillis();
        walBatchCount = 0;
        log.info("WAL 文件已轮转: {}", newFile.getFileName());
    }

    @Override
    public int getQueueSize() {
        return getLogQueue().size();
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
        // 确保队列已初始化
        getLogQueue();

        // 1. 启动定时批量处理任务（处理内存队列中的日志）
        processLogsFuture = ThreadUtils.scheduleWithFixedDelayFuture(() -> {
            try {
                processLogs();
            } catch (Exception e) {
                log.error("批量处理任务执行失败", e);
            }
        }, getProperties().getBatchInterval(), TimeUnit.MILLISECONDS);
        log.info("日志批量处理任务已启动，间隔: {} ms", getProperties().getBatchInterval());

        // 2. 启动 WAL 恢复定时任务（扫描并重试已轮转的 WAL 文件）
        if (getProperties().isWalEnabled() && getProperties().isWalRecoveryEnabled()) {
            walRecoveryFuture = ThreadUtils.scheduleWithFixedDelayFuture(() -> {
                // 并发控制：仅允许一个恢复任务执行
                if (walRecoveryRunning.compareAndSet(false, true)) {
                    try {
                        recoverWalFiles();
                    } catch (Exception e) {
                        log.error("WAL 恢复任务执行失败", e);
                    } finally {
                        walRecoveryRunning.set(false);
                    }
                } else {
                    log.debug("上一次 WAL 恢复任务仍在执行，跳过本次调度");
                }
            }, getProperties().getWalRecoveryInterval(), TimeUnit.MILLISECONDS);
            log.info("WAL 恢复任务已启动，扫描间隔: {} ms", getProperties().getWalRecoveryInterval());
        }
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
     * 当某批次持久化部分失败时，停止处理该文件并保留，等待下次恢复（依赖幂等）。
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
                LogDataEvent event = parseWalLine(line, file.getFileName().toString(), lineNumber);
                if (event == null) {
                    deadCount++;
                    continue;
                }
                totalValidCount++;
                batch.add(event);
                if (batch.size() >= getProperties().getBatchSize()) {
                    int sent = tryPersistBatch(batch);
                    successCount += sent;
                    if (sent < batch.size()) {
                        // 部分失败，停止处理该文件，保留未成功部分
                        log.warn("WAL 批次部分持久化失败，停止处理文件 {} (成功:{}/批次大小:{})", file.getFileName(), sent, batch.size());
                        return; // 保留文件，下次恢复会重试（依赖幂等）
                    }
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                int sent = tryPersistBatch(batch);
                successCount += sent;
                if (sent < batch.size()) {
                    log.warn("WAL 最后批次部分持久化失败，保留文件 {}", file.getFileName());
                    return;
                }
            }

            // 全部有效日志成功则删除文件
            if (successCount == totalValidCount && totalValidCount > 0) {
                Files.deleteIfExists(file);
                log.info("WAL 文件恢复完成，已删除: {}, 成功: {}/{}, 死信: {}", file.getFileName(), successCount, totalValidCount, deadCount);
            } else {
                log.warn("WAL 文件部分恢复失败，保留文件: {}, 成功: {}, 有效总数: {}, 死信: {}", file.getFileName(), successCount, totalValidCount, deadCount);
            }
        } catch (IOException e) {
            log.error("读取 WAL 文件失败: {}", file, e);
        }
    }

    /**
     * 解析单行 WAL 日志，失败时写入死信队列
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
     */
    private void writeToDeadLetter(String originalLine, String sourceFile, int lineNumber) {
        if (!getProperties().isDeadLetterEnabled()) {
            return;
        }
        try {
            Path dir = Paths.get(getProperties().getDeadLetterDir());
            Files.createDirectories(dir);
            String dateStr = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
            Path deadFile = dir.resolve("deadletter-" + dateStr + ".log");

            String record = String.format("{\"timestamp\":\"%s\",\"sourceFile\":\"%s\",\"lineNumber\":%d,\"rawData\":%s}", new java.util.Date(), sourceFile, lineNumber, originalLine);

            Files.write(deadFile, (record + System.lineSeparator()).getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            log.debug("已将损坏日志写入死信队列: {}", deadFile.getFileName());
        } catch (IOException e) {
            log.error("写入死信队列失败", e);
        }
    }

    /**
     * 尝试持久化一个批次，失败时返回 0
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
     */
    @Override
    public void destroy() throws Exception {
        running.set(false);

        // 取消定时任务
        if (processLogsFuture != null) {
            processLogsFuture.cancel(false);
            log.debug("批量处理定时任务已取消");
        }
        if (walRecoveryFuture != null) {
            walRecoveryFuture.cancel(false);
            log.debug("WAL 恢复定时任务已取消");
        }

        // 尝试处理队列中剩余的日志（给予最多5秒窗口）
        log.info("开始处理停机前队列中剩余的 {} 个批次", getLogQueue().size());
        long deadline = System.currentTimeMillis() + 5000;
        while (!getLogQueue().isEmpty() && System.currentTimeMillis() < deadline) {
            processLogs();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (!getLogQueue().isEmpty()) {
            log.warn("停机时队列仍有 {} 个批次未处理，将丢失", getLogQueue().size());
        }

        // 关闭 WAL 写入器（强制 flush）
        walLock.lock();
        try {
            if (walWriter != null) {
                walWriter.flush();
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