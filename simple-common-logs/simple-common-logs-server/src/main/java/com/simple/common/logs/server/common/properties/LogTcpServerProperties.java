package com.simple.common.logs.server.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * TCP日志服务端配置属性
 *
 * @author qty
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "simple.logs.server")
public class LogTcpServerProperties {

    /**
     * 服务端监听端口
     */
    private int port = 9999;

    /**
     * Boss线程数（处理连接）
     */
    private int bossThreads = 1;

    /**
     * Worker线程数（处理IO）
     */
    private int workerThreads = 4;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 30000;

    /**
     * 最大连接数
     */
    private int maxConnections = 1000;

    /**
     * 日志批量处理间隔（毫秒）
     */
    private int batchInterval = 1000;

    /**
     * 日志批量处理大小
     */
    private int batchSize = 1000;

    /**
     * 读空闲超时时间（秒）
     */
    private int readerIdleTime = 10;
}