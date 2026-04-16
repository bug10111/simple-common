package com.simple.common.logs.client.manager;

import com.simple.common.core.utils.SerializeUtils;
import com.simple.common.logs.client.common.manager.LogManager;
import com.simple.common.logs.client.common.properties.LogTcpClientProperties;
import com.simple.common.logs.client.common.tcp.LogProtobufTcpClient;
import com.simple.common.logs.proto.LogBatch;
import com.simple.common.logs.proto.common.event.LogDataEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 带缓冲和失败降级的日志管理器（仅支持批量发送）
 * <p>
 * 功能特性：
 * 1. 内存缓冲队列，后台线程批量发送，减少网络和编解码开销
 * 2. 发送失败时自动降级写入本地文件，待连接恢复后补发
 * 3. 支持优雅停机，确保数据不丢
 * 4. 降级文件采用魔数校验，具备损坏恢复能力
 * </p>
 * <p>
 * 修改说明：
 * - 使用发送回调机制准确捕获发送失败，进行降级处理。
 * - 降级恢复时实现健壮的魔数扫描恢复算法，避免单条记录损坏导致整文件丢弃。
 * - 成功发送的降级文件移动至 sent/ 子目录，仅清理已发送的文件，避免数据丢失。
 * - 【性能优化】降级写入改为异步线程池，避免阻塞业务线程。
 * - 【性能优化】发送失败回调中的反序列化提交到异步线程池，避免阻塞 Netty I/O 线程。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class BufferedLogManager implements LogManager, LogProtobufTcpClient.SendFailureListener {

    /**
     * 降级文件记录魔数，用于校验记录完整性
     */
    private static final int FALLBACK_MAGIC = 0xCAFEBABE;

    private final LogProtobufTcpClient tcpClient;

    private final LogTcpClientProperties properties;

    /**
     * 日志缓冲队列
     */
    private final LinkedBlockingQueue<LogDataEvent> bufferQueue;

    /**
     * 后台发送线程
     */
    private Thread senderThread;

    /**
     * 降级恢复发送线程
     */
    private Thread fallbackResendThread;

    /**
     * 是否正在运行
     */
    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * 降级写入锁，保证文件写入线程安全
     */
    private final ReentrantLock fallbackLock = new ReentrantLock();

    /**
     * 当前降级文件
     */
    private File currentFallbackFile;

    /**
     * 降级文件输出流（二进制流）
     * 使用 volatile 保证多线程可见性
     */
    private volatile DataOutputStream fallbackOutputStream;

    /**
     * 异步降级写入线程池（单线程保证顺序，避免竞争）
     */
    private final ExecutorService fallbackExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "log-fallback-writer");
        t.setDaemon(true);
        return t;
    });

    @Autowired
    public BufferedLogManager(LogProtobufTcpClient tcpClient, LogTcpClientProperties properties) {
        this.tcpClient = tcpClient;
        this.properties = properties;
        this.bufferQueue = new LinkedBlockingQueue<>(properties.getBufferCapacity());
    }

    /**
     * 启动后台线程和降级恢复线程
     */
    @PostConstruct
    @Override
    public void start() {

        // 初始化降级目录
        if (properties.isFallbackEnabled()) {
            initFallbackDirectory();
        }

        // 启动批量发送线程
        senderThread = new Thread(this::batchSendLoop, "Log-Sender");
        senderThread.setDaemon(true);
        senderThread.start();

        // 启动降级恢复线程（定时扫描并重发）
        if (properties.isFallbackEnabled() && properties.isFallbackResendEnabled()) {
            fallbackResendThread = new Thread(this::fallbackResendLoop, "Log-Fallback-Resender");
            fallbackResendThread.setDaemon(true);
            fallbackResendThread.start();
        }

        log.info("BufferedLogManager 启动完成，缓冲容量: {}, 批量大小: {}, 降级启用: {}, 请求体缓存限制: {} bytes, 降级文件保留天数: {}",
                 properties.getBufferCapacity(), properties.getBatchSize(), properties.isFallbackEnabled(),
                 properties.getMaxBodyCacheSize(), properties.getFallbackFileRetentionDays());
    }

    /**
     * 初始化降级存储目录
     */
    private void initFallbackDirectory() {
        Path dir = Paths.get(properties.getFallbackDir());
        try {
            Files.createDirectories(dir);
            // 创建已发送文件目录
            Path sentDir = dir.resolve("sent");
            Files.createDirectories(sentDir);
            // 打开当前写入文件（二进制格式）
            currentFallbackFile = dir.resolve("fallback.log").toFile();
            fallbackOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(currentFallbackFile, true)));
            log.info("降级存储目录初始化完成: {}", dir.toAbsolutePath());
        } catch (IOException e) {
            log.error("初始化降级目录失败", e);
        }
    }

    /**
     * 发送单条日志（异步入队）
     * <p>
     * 若入队成功，对象所有权转移至队列消费者，调用方无需关心对象回收；
     * 若入队失败（队列满），则异步降级写入本地文件，不阻塞调用线程。
     * </p>
     *
     * @param logData 日志数据
     * @return true-入队成功，false-入队失败（队列满）
     */
    @Override
    public boolean send(LogDataEvent logData) {
        // 入队
        boolean offered = bufferQueue.offer(logData);
        if (!offered) {
            log.warn("缓冲队列已满，异步降级写入: traceId={}", logData.getTraceId());
            // 异步降级写入，避免阻塞调用线程
            if (properties.isFallbackEnabled()) {
                fallbackExecutor.submit(() -> writeToFallback(logData));
            } else {
                // 未启用降级则直接回收对象
                logData.recycle();
            }
            return false;
        }
        return true;
    }

    /**
     * 后台批量发送循环
     */
    private void batchSendLoop() {
        List<LogDataEvent> batch = new ArrayList<>(properties.getBatchSize());
        long lastFlushTime = System.currentTimeMillis();

        while (running.get()) {
            try {
                // 从队列中拉取数据，最多等待 flushInterval 时间
                LogDataEvent event = bufferQueue.poll(properties.getBatchFlushInterval(), TimeUnit.MILLISECONDS);
                long now = System.currentTimeMillis();

                if (event != null) {
                    batch.add(event);
                }

                // 达到批量大小或等待超时且有数据，则执行发送
                boolean shouldFlush = batch.size() >= properties.getBatchSize()
                        || (!batch.isEmpty() && (now - lastFlushTime) >= properties.getBatchFlushInterval());

                if (shouldFlush) {
                    sendBatch(batch);
                    batch.clear();
                    lastFlushTime = now;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("批量发送线程被中断");
                break;
            } catch (Exception e) {
                log.error("批量发送循环异常", e);
            }
        }

        // 停机前发送剩余数据
        if (!batch.isEmpty()) {
            sendBatch(batch);
        }
        log.info("批量发送线程退出");
    }

    /**
     * 批量发送日志数据（使用 LogBatch 协议）
     * <p>
     * 将整个批次转换为单个 LogBatch 消息，减少网络包和编解码开销。
     * 发送完成后将事件对象归还到对象池。
     * </p>
     *
     * @param batch 待发送的日志事件列表
     */
    private void sendBatch(List<LogDataEvent> batch) {
        if (batch.isEmpty()) {
            return;
        }

        try {
            // 将整个批次转换为单个 LogBatch 消息
            LogBatch logBatch = LogDataEvent.toBatchProto(batch);
            // 使用带回调的发送方法，失败时触发 onSendFailure 进行降级
            tcpClient.sendBatch(logBatch, this);
        } catch (Exception e) {
            log.error("批量发送异常, 数量: {}, 将降级写入本地", batch.size(), e);
            // 异常情况直接降级
            if (properties.isFallbackEnabled()) {
                for (LogDataEvent event : batch) {
                    writeToFallback(event);
                }
            }
        } finally {
            // 无论成功与否，都回收对象（当前实现中 recycle 为空操作）
            for (LogDataEvent event : batch) {
                event.recycle();
            }
        }
    }

    /**
     * 发送失败回调（实现自 SendFailureListener）
     * <p>
     * 当 Netty 写操作失败时触发，将批次内的所有日志降级写入本地文件。
     * 注意：该方法可能在 Netty I/O 线程中执行，为避免阻塞，将反序列化和写文件提交到异步线程池。
     * </p>
     *
     * @param batch 发送失败的批量日志
     */
    @Override
    public void onSendFailure(LogBatch batch) {
        if (!properties.isFallbackEnabled()) {
            return;
        }
        log.warn("批量日志发送失败，触发降级，数量: {}", batch.getLogsCount());
        // 提交到异步线程池，避免阻塞 Netty I/O 线程
        fallbackExecutor.submit(() -> {
            List<LogDataEvent> events = LogDataEvent.fromBatchProto(batch);
            for (LogDataEvent event : events) {
                writeToFallback(event);
                event.recycle();
            }
        });
    }

    /**
     * 降级写入本地文件（使用 Protobuf 二进制格式 + 魔数校验）
     * <p>
     * 每条日志写入格式：4字节魔数(0xCAFEBABE) + 4字节长度 + protobuf二进制数据。
     * 引入魔数可在恢复时跳过损坏记录，提高鲁棒性。
     * </p>
     *
     * @param event 日志事件
     */
    private void writeToFallback(LogDataEvent event) {
        fallbackLock.lock();
        try {
            // 若写入器为空，尝试重新初始化
            DataOutputStream out = this.fallbackOutputStream;
            if (out == null) {
                try {
                    initFallbackDirectory();
                    out = this.fallbackOutputStream;
                } catch (Exception e) {
                    log.error("降级目录重新初始化失败，无法写入降级日志", e);
                    return;
                }
            }
            if (out == null) {
                return;
            }
            // 将 Protobuf 对象序列化为字节数组
            byte[] data = SerializeUtils.serialize(event);
            // 写入魔数、长度和数据
            out.writeInt(FALLBACK_MAGIC);
            out.writeInt(data.length);
            out.write(data);
            out.flush();

            // 检查文件大小，超过限制则轮转
            if (currentFallbackFile.length() > properties.getFallbackFileMaxSize()) {
                rotateFallbackFile();
            }

        } catch (IOException e) {
            log.error("降级写入失败", e);
            // 写入异常时关闭并置空写入器，下次写入时重新初始化
            try {
                if (fallbackOutputStream != null) {
                    fallbackOutputStream.close();
                }
            } catch (IOException ex) {
                // 忽略关闭异常
            }
            fallbackOutputStream = null;
        } finally {
            fallbackLock.unlock();
        }
    }

    /**
     * 轮转降级文件
     * 注意：此方法在持有 fallbackLock 锁的情况下调用，保证原子性
     */
    private void rotateFallbackFile() throws IOException {
        DataOutputStream oldOut = this.fallbackOutputStream;
        if (oldOut != null) {
            oldOut.close();
        }
        Path dir = Paths.get(properties.getFallbackDir());
        String timestamp = String.valueOf(System.currentTimeMillis());
        Path newFile = dir.resolve("fallback-" + timestamp + ".log");
        Files.move(currentFallbackFile.toPath(), newFile, StandardCopyOption.ATOMIC_MOVE);
        currentFallbackFile = dir.resolve("fallback.log").toFile();
        fallbackOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(currentFallbackFile, true)));
        log.info("降级文件已轮转: {}", newFile.getFileName());
    }

    /**
     * 降级恢复重发循环
     * <p>
     * 定期扫描降级目录下已完成轮转的文件，尝试将其中日志重发到服务端。
     * 同时清理 sent/ 目录下超过保留期限的已发送文件。
     * </p>
     */
    private void fallbackResendLoop() {
        while (running.get()) {
            try {
                if (tcpClient.isActive()) {
                    resendFallbackFiles();
                }
                // 清理已成功发送的过期文件
                cleanExpiredSentFiles();
                Thread.sleep(60000); // 每分钟检查一次
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("降级恢复循环异常", e);
            }
        }
    }

    /**
     * 重发已轮转的降级文件（带限流控制）
     */
    private void resendFallbackFiles() {
        Path dir = Paths.get(properties.getFallbackDir());
        int maxFilesPerRound = properties.getFallbackResendMaxFilesPerRound();
        int processed = 0;

        try (var stream = Files.newDirectoryStream(dir, "fallback-*.log")) {
            for (Path file : stream) {
                // 限流检查：一次扫描最多处理指定数量的文件
                if (maxFilesPerRound > 0 && processed >= maxFilesPerRound) {
                    log.debug("本次降级恢复已达到处理上限 {} 个文件，剩余文件下次处理", maxFilesPerRound);
                    break;
                }
                boolean success = resendFile(file);
                if (success) {
                    processed++;
                }
            }
        } catch (IOException e) {
            log.error("扫描降级文件失败", e);
        }
    }

    /**
     * 重发单个降级文件（使用 RandomAccessFile 实现健壮的魔数扫描恢复）
     * <p>
     * 文件中每条记录格式：4字节魔数 + 4字节长度 + protobuf二进制数据。
     * 采用魔数扫描恢复机制：当遇到无效魔数时，逐字节跳过直到找到下一个魔数，最大化恢复数据。
     * 采用分批发送策略，内存占用可控。
     * 发送成功后，将文件移动到 sent/ 子目录，而非直接删除。
     * </p>
     *
     * @param file 文件路径
     * @return true-文件处理成功并已移动，false-处理失败（保留原文件）
     */
    private boolean resendFile(Path file) {
        log.info("开始恢复发送降级文件: {}", file.getFileName());
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            List<LogDataEvent> batch = new ArrayList<>(properties.getBatchSize());
            int successCount = 0;
            int totalCount = 0;
            long filePointer = 0;

            while (filePointer < raf.length()) {
                raf.seek(filePointer);
                try {
                    int magic = raf.readInt();
                    if (magic != FALLBACK_MAGIC) {
                        // 魔数不匹配，跳过1字节继续寻找
                        log.debug("魔数校验失败，跳过1字节继续扫描，当前位置: {}", filePointer);
                        filePointer += 1;
                        continue;
                    }
                    int length = raf.readInt();
                    if (length <= 0 || length > 10 * 1024 * 1024) { // 限制最大10MB防止异常
                        log.warn("无效记录长度: {}，跳过1字节继续扫描", length);
                        filePointer += 1;
                        continue;
                    }
                    // 检查剩余文件长度是否足够
                    if (filePointer + 8 + length > raf.length()) {
                        log.warn("文件截断，剩余长度不足，停止读取");
                        break;
                    }
                    byte[] data = new byte[length];
                    raf.readFully(data);
                    LogDataEvent event = SerializeUtils.deserialize(data, LogDataEvent.class);
                    batch.add(event);
                    totalCount++;
                    // 更新文件指针到当前记录末尾
                    filePointer = raf.getFilePointer();

                    if (batch.size() >= properties.getBatchSize()) {
                        int sent = resendBatch(batch);
                        successCount += sent;
                        if (sent < batch.size()) {
                            // 部分发送失败，停止处理该文件，保留未成功部分
                            log.warn("批次部分发送失败，停止处理文件 {}", file.getFileName());
                            return false;
                        }
                        batch.clear();
                    }
                } catch (EOFException e) {
                    break;
                } catch (Exception e) {
                    log.warn("解析记录失败，跳过1字节继续，位置: {}", filePointer, e);
                    filePointer += 1;
                }
            }
            // 处理剩余批次
            if (!batch.isEmpty()) {
                int sent = resendBatch(batch);
                successCount += sent;
                if (sent < batch.size()) {
                    log.warn("最后批次部分发送失败，保留文件 {}", file.getFileName());
                    return false;
                }
            }

            // 所有日志发送成功，移动文件到 sent/ 目录
            if (successCount == totalCount && totalCount > 0) {
                Path sentDir = Paths.get(properties.getFallbackDir(), "sent");
                Files.createDirectories(sentDir);
                Path target = sentDir.resolve(file.getFileName());
                Files.move(file, target, StandardCopyOption.ATOMIC_MOVE);
                log.info("降级文件恢复发送完成，已移动到: {}, 成功条数: {}/{}", target.getFileName(), successCount, totalCount);
                return true;
            } else {
                log.warn("降级文件发送不完整，保留文件: {}, 成功: {}, 总数: {}", file.getFileName(), successCount, totalCount);
                return false;
            }
        } catch (IOException e) {
            log.error("处理降级文件失败: {}", file, e);
            return false;
        }
    }

    /**
     * 重发一个批次
     *
     * @param batch 批次列表
     * @return 成功发送的条数
     */
    private int resendBatch(List<LogDataEvent> batch) {
        try {
            LogBatch logBatch = LogDataEvent.toBatchProto(batch);
            // 重发时使用无回调方法，失败则返回0（不再进行二次降级，避免死循环）
            boolean success = tcpClient.sendBatch(logBatch);
            return success ? batch.size() : 0;
        } catch (Exception e) {
            log.error("重发降级批次失败", e);
            return 0;
        } finally {
            for (LogDataEvent event : batch) {
                event.recycle();
            }
        }
    }

    /**
     * 清理已成功发送的过期文件（仅清理 sent/ 目录下的文件）
     */
    private void cleanExpiredSentFiles() {
        int retentionDays = properties.getFallbackFileRetentionDays();
        if (retentionDays < 0) {
            return; // 永不过期
        }
        long expireTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays);
        Path sentDir = Paths.get(properties.getFallbackDir(), "sent");
        if (!Files.exists(sentDir)) {
            return;
        }
        try (var stream = Files.newDirectoryStream(sentDir, "fallback-*.log")) {
            for (Path file : stream) {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                    if (attrs.lastModifiedTime().toMillis() < expireTime) {
                        Files.deleteIfExists(file);
                        log.info("删除过期已发送文件: {}", file.getFileName());
                    }
                } catch (IOException e) {
                    log.warn("检查文件属性失败: {}", file, e);
                }
            }
        } catch (IOException e) {
            log.error("清理过期已发送文件失败", e);
        }
    }

    /**
     * 停止日志客户端，优雅关闭
     */
    @PreDestroy
    @Override
    public void stop() {
        running.set(false);

        // 中断发送线程，使其尽快退出阻塞的 poll
        if (senderThread != null) {
            senderThread.interrupt();
            try {
                senderThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 排空队列中剩余的所有日志并尝试最后一次发送
        List<LogDataEvent> remaining = new ArrayList<>();
        bufferQueue.drainTo(remaining);
        if (!remaining.isEmpty()) {
            log.info("停机前处理队列中剩余的 {} 条日志", remaining.size());
            sendBatch(remaining);
        }

        // 等待降级恢复线程结束
        if (fallbackResendThread != null) {
            fallbackResendThread.interrupt();
            try {
                fallbackResendThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 关闭异步降级线程池
        fallbackExecutor.shutdown();
        try {
            if (!fallbackExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                fallbackExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            fallbackExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 关闭降级文件输出流
        if (fallbackOutputStream != null) {
            try {
                fallbackOutputStream.close();
            } catch (IOException e) {
                log.warn("关闭降级输出流失败", e);
            }
        }

        log.info("BufferedLogManager 已停止");
    }

    /**
     * 获取当前队列大小
     */
    public int getQueueSize() {
        return bufferQueue.size();
    }
}