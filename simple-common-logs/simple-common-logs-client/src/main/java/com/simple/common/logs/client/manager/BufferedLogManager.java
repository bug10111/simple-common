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

    /**
     * 重发等待写入确认的超时秒数
     */
    private static final long RESEND_ACK_TIMEOUT_SECONDS = 30;

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
     * 异步降级写入线程池（单线程保证写入顺序，避免竞争）。
     * 有界队列 + 饱和告警拒绝：磁盘慢/文件竞争时无界队列会持续堆积耗尽堆内存
     */
    private final ExecutorService fallbackExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1000), task -> {
        Thread worker = new Thread(task, "log-fallback-writer");
        worker.setDaemon(true);
        return worker;
    }, (task, pool) -> log.error("降级写入线程池已饱和，拒绝 1 个降级写入任务"));

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
    /**
     * 发送失败回调（实现自 SendFailureListener）。
     * <p>
     * 可能在 Netty IO 线程执行：反序列化与文件写入提交至异步线程池；
     * 线程池已停机时禁止提交（RejectedExecutionException 会以未捕获异常出现在 IO 线程），
     * 此时显式告警丢弃，保证丢弃可观测。
     * </p>
     *
     * @param batch 发送失败的批量日志
     */
    @Override
    public void onSendFailure(LogBatch batch) {
        if (!properties.isFallbackEnabled()) {
            return;
        }
        int count = batch.getLogsCount();
        log.warn("批量日志发送失败，触发降级，数量: {}", count);
        if (!running.get() || fallbackExecutor.isShutdown()) {
            log.error("降级线程池已关闭，无法写入降级日志，丢弃 {} 条日志", count);
            return;
        }
        try {
            fallbackExecutor.submit(() -> {
                List<LogDataEvent> events = LogDataEvent.fromBatchProto(batch);
                for (LogDataEvent event : events) {
                    writeToFallback(event);
                    event.recycle();
                }
            });
        } catch (RejectedExecutionException e) {
            log.error("降级线程池拒绝任务，丢失 {} 条日志", count);
        }
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
            long committedPointer = 0;
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
                        committedPointer = filePointer;
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
                    return finishPartialResend(file, committedPointer);
                }
                committedPointer = filePointer;
            }

            boolean readToEnd = filePointer >= Files.size(file);
            if (totalCount == 0) {
                // 文件内无任何可解析记录（空文件/全部损坏）：
                // 空文件直接删除；非空则隔离归档，避免每轮重复扫描与死信重复放大
                if (Files.size(file) == 0) {
                    Files.deleteIfExists(file);
                } else {
                    Path corrupt = file.resolveSibling(file.getFileName() + ".corrupt");
                    Files.move(file, corrupt, StandardCopyOption.ATOMIC_MOVE);
                    log.warn("降级文件无有效记录，已隔离: {}", corrupt.getFileName());
                }
                return true;
            }
            if (successCount == totalCount) {
                if (!readToEnd) {
                    // 存在未读取的损坏尾部（崩溃截断产物）：先归档尾部字节再归档文件，
                    // 避免已发送成功却静默丢弃尾部数据
                    quarantineTail(file, filePointer);
                }
                Path sentDir = Paths.get(properties.getFallbackDir(), "sent");
                Files.createDirectories(sentDir);
                Path target = sentDir.resolve(file.getFileName());
                Files.move(file, target, StandardCopyOption.ATOMIC_MOVE);
                log.info("降级文件恢复发送完成，已移动到: {}, 成功条数: {}/{}", target.getFileName(), successCount, totalCount);
                return true;
            }
            log.warn("降级文件发送不完整，保留文件: {}, 成功: {}, 总数: {}", file.getFileName(), successCount, totalCount);
            return false;
        } catch (IOException e) {
            log.error("处理降级文件失败: {}", file, e);
            return false;
        }
    }
    /**
     * 部分发送失败后的收尾处理。
     * <p>
     * 已有完整批次发送成功（committedPointer > 0）时：将剩余未发送字节重写为新的
     * 降级文件、已发送前缀归档至 sent/，修复下轮从文件头重放导致前缀重复发送的问题；
     * 尚无成功批次时保留原文件，等待下轮整体重试。
     * </p>
     *
     * @param file 降级文件
     * @param committedPointer 已成功发送批次的最高文件偏移
     * @return 是否已归档（计入本轮处理限额）
     */
    private boolean finishPartialResend(Path file, long committedPointer) {
        try {
            if (committedPointer <= 0) {
                return false;
            }
            long fileLength = Files.size(file);
            if (committedPointer < fileLength) {
                rewriteRemaining(file, committedPointer);
            }
            Path sentDir = Paths.get(properties.getFallbackDir(), "sent");
            Files.createDirectories(sentDir);
            Path target = sentDir.resolve(file.getFileName());
            Files.move(file, target, StandardCopyOption.ATOMIC_MOVE);
            log.info("已发送前缀归档至: {}, 剩余部分已重写为新降级文件", target.getFileName());
            return true;
        } catch (IOException e) {
            log.error("归档已发送前缀失败，保留原文件下轮整体重发: {}", file, e);
            return false;
        }
    }
    /**
     * 将文件内 fromPointer 起的剩余字节重写为新的降级文件，参与下轮重发
     */
    private void rewriteRemaining(Path file, long fromPointer) throws IOException {
        Path target = file.getParent().resolve("fallback-" + System.currentTimeMillis() + ".log");
        try (RandomAccessFile source = new RandomAccessFile(file.toFile(), "r")) {
            source.seek(fromPointer);
            try (java.io.OutputStream out = Files.newOutputStream(target,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = source.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
            }
        }
        log.warn("未发送剩余部分已重写为新降级文件: {}, 起始偏移: {}", target.getFileName(), fromPointer);
    }

    /**
     * 将文件尾部（fromPointer 起）未读取/未解析的字节归档为 .corrupt 文件，
     * 避免静默丢弃（崩溃截断产生的半条记录保留待人工排查）
     */
    private void quarantineTail(Path file, long fromPointer) {
        try (RandomAccessFile source = new RandomAccessFile(file.toFile(), "r")) {
            source.seek(fromPointer);
            Path corrupt = file.resolveSibling(file.getFileName() + ".corrupt");
            try (java.io.OutputStream out = Files.newOutputStream(corrupt,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = source.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
            }
            log.warn("未读取尾部已归档: {}", corrupt.getFileName());
        } catch (IOException e) {
            log.error("归档降级文件尾部失败: {}", file, e);
        }
    }
    /**
     * 重发一个批次。
     * <p>
     * 通过监听器回调等待真实送达结果：sendBatch 返回 true 仅代表写操作已提交，
     * 直接按成功处理会把降级文件过早移入 sent/ 并被清理，造成已提交未送达的日志静默丢失。
     * 以有界超时等待，超时按失败处理（文件保留，等待下轮重发）。
     * </p>
     *
     * @param batch 批次列表
     * @return 成功发送的条数
     */
    private int resendBatch(List<LogDataEvent> batch) {
        try {
            LogBatch logBatch = LogDataEvent.toBatchProto(batch);
            if (!tcpClient.isActive()) {
                // 通道不可用直接按失败返回（保留文件），不做二次降级避免死循环
                return 0;
            }
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean delivered = new AtomicBoolean(false);
            boolean submitted = tcpClient.sendBatch(logBatch, new LogProtobufTcpClient.SendFailureListener() {
                @Override
                public void onSendFailure(LogBatch b) {
                    latch.countDown();
                }

                @Override
                public void onSendSuccess(LogBatch b) {
                    delivered.set(true);
                    latch.countDown();
                }
            });
            if (!submitted) {
                return 0;
            }
            boolean finished = latch.await(RESEND_ACK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                log.warn("等待重发结果超时（{}s），按失败处理，保留降级文件, 批次条数: {}", RESEND_ACK_TIMEOUT_SECONDS, batch.size());
                return 0;
            }
            return delivered.get() ? batch.size() : 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
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
     * 停止日志客户端，优雅关闭。
     * <p>
     * 顺序：停发送线程 → 按 batchSize 分批排空队列 → 停重发线程 → 关线程池 →
     * 持锁关闭降级输出流（防止与在途写入竞争产生半条记录）→
     * 轮转活跃降级文件（本次运行写入活跃文件的残留数据下次启动可被恢复重发）。
     * </p>
     */
    @PreDestroy
    @Override
    public void stop() {
        running.set(false);

        if (senderThread != null) {
            senderThread.interrupt();
            try {
                senderThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 排空队列剩余日志并按 batchSize 分批发送：修复一次性 drainTo 产生超大批次的问题
        List<LogDataEvent> remaining = new ArrayList<>(properties.getBatchSize());
        int drained;
        boolean firstDrain = true;
        do {
            remaining.clear();
            drained = bufferQueue.drainTo(remaining, properties.getBatchSize());
            if (!remaining.isEmpty()) {
                if (firstDrain) {
                    log.info("停机前处理队列中剩余 {} 条日志", bufferQueue.size() + remaining.size());
                    firstDrain = false;
                }
                sendBatch(remaining);
            }
        } while (drained > 0);

        if (fallbackResendThread != null) {
            fallbackResendThread.interrupt();
            try {
                fallbackResendThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        fallbackExecutor.shutdown();
        try {
            if (!fallbackExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                fallbackExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            fallbackExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 关闭降级文件输出流：必须持有 fallbackLock，防止与在途写入竞争产生半条记录
        fallbackLock.lock();
        try {
            if (fallbackOutputStream != null) {
                try {
                    fallbackOutputStream.close();
                } catch (IOException e) {
                    log.warn("关闭降级输出流失败", e);
                }
                fallbackOutputStream = null;
            }
        } finally {
            fallbackLock.unlock();
        }

        // 轮转活跃降级文件：停机期间写入活跃文件的残留数据下次启动可被恢复重发
        rotateStaleFallbackFile();

        log.info("BufferedLogManager 已停止");
    }

    /**
     * 获取当前队列大小
     *
     * @return 队列大小
     */
    public int getQueueSize() {
        return bufferQueue.size();
    }

    /**
     * 轮转上次进程遗留的活跃降级文件 fallback.log。
     * <p>
     * 崩溃/停机时写入活跃文件的记录只有等新一轮写入触发按大小轮转后才会被
     * 恢复线程扫描，应用空闲时这些记录将永久滞留（使用方视角即数据丢失）。
     * 启动时将非空活跃文件改名，使其进入恢复重发扫描范围。
     * </p>
     */
    private void rotateStaleFallbackFile() {
        fallbackLock.lock();
        try {
            Path active = Paths.get(properties.getFallbackDir(), "fallback.log");
            if (Files.exists(active) && Files.size(active) > 0) {
                Path target = active.getParent().resolve("fallback-" + System.currentTimeMillis() + ".log");
                Files.move(active, target, StandardCopyOption.ATOMIC_MOVE);
                log.info("活跃降级文件已轮转: {}", target.getFileName());
            }
        } catch (IOException e) {
            log.error("轮转活跃降级文件失败", e);
        } finally {
            fallbackLock.unlock();
        }
    }
}
