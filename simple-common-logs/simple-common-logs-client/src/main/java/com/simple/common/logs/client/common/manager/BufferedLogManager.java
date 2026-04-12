package com.simple.common.logs.client.common.manager;

import cn.hutool.json.JSONUtil;
import com.simple.common.logs.proto.common.event.LogDataEvent;
import com.simple.common.logs.client.common.properties.LogTcpClientProperties;
import com.simple.common.logs.client.common.tcp.LogProtobufTcpClient;
import com.simple.common.logs.proto.LogBatch;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 带缓冲和失败降级的日志管理器（仅支持批量发送）
 * <p>
 * 功能特性：
 * 1. 内存缓冲队列，后台线程批量发送，减少网络和编解码开销
 * 2. 发送失败时自动降级写入本地文件，待连接恢复后补发
 * 3. 支持优雅停机，确保数据不丢
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class BufferedLogManager implements LogManager {

    @Autowired
    private LogProtobufTcpClient tcpClient;

    @Autowired
    private LogTcpClientProperties properties;

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
     * 降级文件输出流
     */
    private BufferedWriter fallbackWriter;

    public BufferedLogManager(LogTcpClientProperties properties) {
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

        log.info("BufferedLogManager 启动完成，缓冲容量: {}, 批量大小: {}, 降级启用: {}", properties.getBufferCapacity(), properties.getBatchSize(), properties.isFallbackEnabled());
    }

    /**
     * 初始化降级存储目录
     */
    private void initFallbackDirectory() {
        Path dir = Paths.get(properties.getFallbackDir());
        try {
            Files.createDirectories(dir);
            // 打开当前写入文件
            currentFallbackFile = dir.resolve("fallback.log").toFile();
            fallbackWriter = new BufferedWriter(new FileWriter(currentFallbackFile, true));
            log.info("降级存储目录初始化完成: {}", dir.toAbsolutePath());
        } catch (IOException e) {
            log.error("初始化降级目录失败", e);
        }
    }

    /**
     * 发送单条日志（异步入队）
     *
     * @param logData 日志数据
     * @return true-入队成功，false-入队失败（队列满）
     */
    @Override
    public boolean send(LogDataEvent logData) {
        // 入队
        boolean offered = bufferQueue.offer(logData);
        if (!offered) {
            log.warn("缓冲队列已满，丢弃日志: traceId={}", logData.getTraceId());
            // 尝试降级写入
            if (properties.isFallbackEnabled()) {
                writeToFallback(logData);
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
                boolean shouldFlush = batch.size() >= properties.getBatchSize() || (!batch.isEmpty() && (now - lastFlushTime) >= properties.getBatchFlushInterval());

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
            tcpClient.sendBatch(logBatch);
            log.debug("批量发送成功, 数量: {}", batch.size());
        } catch (Exception e) {
            log.error("批量发送失败, 数量: {}, 将降级写入本地", batch.size(), e);

            // 降级写入
            if (properties.isFallbackEnabled()) {
                for (LogDataEvent event : batch) {
                    writeToFallback(event);
                }
            }
        } finally {
            // 无论成功与否，都回收对象到池中（降级写入时已序列化为 JSON，对象可回收）
            for (LogDataEvent event : batch) {
                event.recycle();
            }
        }
    }

    /**
     * 降级写入本地文件
     * <p>
     * 将日志事件序列化为 JSON 行写入降级文件，保证即使 TCP 不可用也不丢数据。
     * </p>
     *
     * @param event 日志事件
     */
    private void writeToFallback(LogDataEvent event) {
        fallbackLock.lock();
        try {
            if (fallbackWriter == null) {
                return;
            }
            // 使用 Hutool JSON 序列化
            String json = JSONUtil.toJsonStr(event);
            fallbackWriter.write(json);
            fallbackWriter.newLine();
            fallbackWriter.flush();

            // 检查文件大小，超过限制则轮转
            if (currentFallbackFile.length() > properties.getFallbackFileMaxSize()) {
                rotateFallbackFile();
            }

        } catch (IOException e) {
            log.error("降级写入失败", e);
        } finally {
            fallbackLock.unlock();
        }
    }

    /**
     * 轮转降级文件
     */
    private void rotateFallbackFile() throws IOException {
        fallbackWriter.close();
        Path dir = Paths.get(properties.getFallbackDir());
        String timestamp = String.valueOf(System.currentTimeMillis());
        Path newFile = dir.resolve("fallback-" + timestamp + ".log");
        Files.move(currentFallbackFile.toPath(), newFile, StandardCopyOption.ATOMIC_MOVE);
        currentFallbackFile = dir.resolve("fallback.log").toFile();
        fallbackWriter = new BufferedWriter(new FileWriter(currentFallbackFile, true));
        log.info("降级文件已轮转: {}", newFile.getFileName());
    }

    /**
     * 降级恢复重发循环
     * <p>
     * 定期扫描降级目录下已完成轮转的文件，尝试将其中日志重发到服务端。
     * </p>
     */
    private void fallbackResendLoop() {
        while (running.get()) {
            try {
                if (tcpClient.isActive()) {
                    resendFallbackFiles();
                }
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
     * 重发已轮转的降级文件
     */
    private void resendFallbackFiles() {
        Path dir = Paths.get(properties.getFallbackDir());
        try (var stream = Files.newDirectoryStream(dir, "fallback-*.log")) {
            for (Path file : stream) {
                resendFile(file);
            }
        } catch (IOException e) {
            log.error("扫描降级文件失败", e);
        }
    }

    /**
     * 重发单个降级文件（流式处理，避免一次性加载大文件导致OOM）
     * <p>
     * 采用逐行读取、分批发送的策略，内存占用可控。
     * </p>
     *
     * @param file 文件路径
     */
    private void resendFile(Path file) {
        log.info("开始恢复发送降级文件: {}", file.getFileName());
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            List<LogDataEvent> batch = new ArrayList<>(properties.getBatchSize());
            int successCount = 0;
            int totalCount = 0; // 新增总行数计数

            while ((line = reader.readLine()) != null) {
                LogDataEvent event = JSONUtil.toBean(line, LogDataEvent.class);
                if (event != null) {
                    batch.add(event);
                    totalCount++;
                    if (batch.size() >= properties.getBatchSize()) {
                        successCount += resendBatch(batch);
                        batch.clear();
                    }
                }
            }
            if (!batch.isEmpty()) {
                successCount += resendBatch(batch);
            }

            // 仅当所有日志都发送成功时才删除文件
            if (successCount == totalCount && totalCount > 0) {
                Files.deleteIfExists(file);
                log.info("降级文件恢复发送完成，已删除: {}, 成功条数: {}/{}", file.getFileName(), successCount, totalCount);
            } else {
                log.warn("降级文件部分发送失败，保留文件: {}, 成功: {}, 总数: {}", file.getFileName(), successCount, totalCount);
            }
        } catch (IOException e) {
            log.error("读取降级文件失败: {}", file, e);
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
            tcpClient.sendBatch(logBatch);
            return batch.size();
        } catch (Exception e) {
            log.error("重发降级批次失败", e);
            return 0;
        }
    }

    /**
     * 停止日志客户端，优雅关闭
     */
    @PreDestroy
    @Override
    public void stop() {
        running.set(false);
        try {
            if (senderThread != null) {
                senderThread.join(5000);
            }
            if (fallbackResendThread != null) {
                fallbackResendThread.join(5000);
            }
            if (fallbackWriter != null) {
                fallbackWriter.close();
            }
        } catch (InterruptedException | IOException e) {
            log.error("关闭 BufferedLogManager 异常", e);
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