package com.simple.common.logs.server.common.config;

import com.simple.common.logs.server.common.manager.LogsSaveManager;
import com.simple.common.logs.server.common.properties.LogTcpServerProperties;
import com.simple.common.logs.server.common.tcp.LogProtobufTcpServer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 日志服务端自动装配。
 * <p>
 * 不再对模块根包做整体组件扫描：组件扫描注册的 Bean 无条件生效、绕过自动装配回退契约，
 * 使用方仅引入 jar 即会绑定 9999 端口且无法关闭。现改为显式声明 Bean：
 * 1. 通过 {@code simple.logs.server.enabled=false} 可整体关闭；
 * 2. {@code LogProtobufTcpServer} 带 {@code @ConditionalOnMissingBean}，使用方可自行覆盖。
 * </p>
 * <p>
 * 要求使用方提供 {@link LogsSaveManager} 实现 Bean（如继承 AbsLogsSaveManager）。
 * </p>
 *
 * @author qty
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "simple.logs.server", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LogTcpServerProperties.class)
public class LogServerConfig {

    /**
     * Protobuf TCP 日志服务端（destroyMethod 对应 {@link LogProtobufTcpServer#shutdown()}）
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public LogProtobufTcpServer logProtobufTcpServer(LogTcpServerProperties properties, LogsSaveManager logsSaveManager) {
        return new LogProtobufTcpServer(properties, logsSaveManager);
    }
}
