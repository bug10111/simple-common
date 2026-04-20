package com.simple.common.redis.common.config;

import cn.hutool.core.util.StrUtil;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by IntelliJ IDEA
 *
 * @author qty
 */
@Configuration
@ComponentScan(basePackages = { "com.simple.common.redis" })
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

    @Value("${spring.data.redis.timeout:3000}")
    private int timeout;

    @Value("${redisson.connection-min-idle-size:10}")
    private int connectionMinimumIdleSize;

    @Bean(destroyMethod = "shutdown")
    public Redisson redisson() {
        String address = "redis://" + host + ":" + port;
        Config config = new Config();
        SingleServerConfig singleServerConfig = config.useSingleServer()
                                                      .setAddress(address)
                                                      .setDatabase(database)
                                                      .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                                                      .setTimeout(timeout);
        if (StrUtil.isNotEmpty(password)) {
            singleServerConfig.setPassword(password);
        }
        return (Redisson) Redisson.create(config);
    }
}
