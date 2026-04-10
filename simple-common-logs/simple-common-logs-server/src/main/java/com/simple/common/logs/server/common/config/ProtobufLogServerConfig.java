package com.simple.common.logs.server.common.config;

import com.simple.common.logs.server.common.manager.LogsSaveManager;
import com.simple.common.logs.server.common.properties.LogTcpServerProperties;
import com.simple.common.logs.server.common.tcp.LogProtobufTcpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Protobuf 日志服务端配置
 *
 * @author Admin
 */
@Configuration
@EnableConfigurationProperties(LogTcpServerProperties.class)
public class ProtobufLogServerConfig {

    @Autowired(required = false)
    private LogsSaveManager logsSaveManager;

    @Autowired
    private LogTcpServerProperties properties;

    @Bean
    public LogProtobufTcpServer logProtobufTcpServer() {
        LogProtobufTcpServer server = new LogProtobufTcpServer();
        server.setProperties(properties);
        server.setLogsSaveManager(logsSaveManager);
        server.start();
        return server;
    }
}