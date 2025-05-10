package com.simple.common.websocket.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.websocket")
public class WebSocketProperties {

    private int port = 5558;

    // 接收连接的线程数
    private int bossThreads = 1;

    // 工作线程数
    private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;

    // 等待连接队列长度
    private int soBacklog = 1024;

    // 写缓冲区高水位
    private int writeBufferHigh = 64 * 1024;

    // 写缓冲区低水位
    private int writeBufferLow = 32 * 1024;

    //5秒静默期
    private int shutdownQuietPeriod = 5000;

    //15秒超时
    private int shutdownTimeOut = 15000;

}
