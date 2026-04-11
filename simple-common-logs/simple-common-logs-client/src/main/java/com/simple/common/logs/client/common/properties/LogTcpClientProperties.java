package com.simple.common.logs.client.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * TCP日志客户端配置属性
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.logs.client")
public class LogTcpClientProperties {


    /**
     * 服务端主机地址
     */
    private String host = "127.0.0.1";

    /**
     * 服务端端口
     */
    private int port = 9999;

    /**
     * 连接超时时间(毫秒)
     */
    private int connectTimeout = 5000;

    /**
     * 重连间隔时间(毫秒)
     */
    private int reconnectInterval = 5000;

    /**
     * 最大重连次数，-1表示无限重连
     */
    private int maxReconnectAttempts = -1;

    /**
     * 工作线程数
     */
    private int workerThreads = 4;
}