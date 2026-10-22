package com.simple.common.redis.common.config;

import cn.hutool.core.util.StrUtil;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Created by IntelliJ IDEA
 *
 * @author qty
 */
@Configuration
@ComponentScan(basePackages = { "com.simple.common.redis" })
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(prefix = "redisson", name = "open", havingValue = "true", matchIfMissing = true)
public class RedissonConfig {

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    /**
     * 命令超时时长：绑定 Spring Boot 的 {@link Duration} 配置（支持 3s/3000ms 等写法），
     * 与 Redisson setTimeout 的毫秒语义显式换算，避免「配置 3s 实际按 3 毫秒」的单位错配
     */
    @Value("${spring.data.redis.timeout:3000ms}")
    private Duration timeout;

    @Value("${redisson.connection-minimum-idle-size:10}")
    private int connectionMinimumIdleSize;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() {
        String address = "redis://" + host + ":" + port;
        Config config = new Config();
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(address)
                .setDatabase(database)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                // Redisson setTimeout 单位为毫秒，显式从 Duration 换算
                .setTimeout((int) timeout.toMillis());
        if (StrUtil.isNotEmpty(password)) {
            singleServerConfig.setPassword(password);
        }
        return Redisson.create(config);
    }
}
