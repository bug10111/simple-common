package com.simple.common.logs.client.common.config;

import com.simple.common.logs.client.common.properties.LogTcpClientProperties;
import com.simple.common.logs.client.common.tcp.LogProtobufTcpClient;
import com.simple.common.logs.client.manager.ProtobufLogSenderManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Protobuf 日志客户端配置
 *
 * @author Admin
 */
@Configuration
@EnableConfigurationProperties(LogTcpClientProperties.class)
public class ProtobufLogClientConfig {

    @Bean
    public LogProtobufTcpClient logProtobufTcpClient(LogTcpClientProperties properties) {
        LogProtobufTcpClient client = new LogProtobufTcpClient();
        client.setHost(properties.getHost());
        client.setPort(properties.getPort());
        client.setReconnectInterval(properties.getReconnectInterval());
        client.setWorkerThreads(properties.getWorkerThreads());
        client.start();
        return client;
    }

    @Bean
    public ProtobufLogSenderManager protobufLogSenderManager(LogProtobufTcpClient client) {
        return new ProtobufLogSenderManager(client);
    }
}