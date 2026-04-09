package com.simple.common.logs.server.common.config;

import com.simple.common.logs.server.common.properties.LogTcpServerProperties;
import com.simple.common.logs.server.common.tcp.LogTcpServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

/**
 * 日志服务端配置类
 *
 * @author qty
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(LogTcpServerProperties.class)
@ConditionalOnProperty(prefix = "simple.logs.tcp.server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LogServerConfig {

    @Autowired(required = false)
    private LogTcpServer logTcpServer;

    @PostConstruct
    public void init() {
        if (logTcpServer != null) {
            logTcpServer.start();
        }
    }

}