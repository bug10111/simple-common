package com.simple.common.redis.common.config;

import cn.hutool.core.util.StrUtil;
import org.redisson.Redisson;
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
@ComponentScan(basePackages = { "com.simple.common.redis.**" })
@ConditionalOnProperty(prefix = "redisson", name = "open", havingValue = "true", matchIfMissing = true)
public class RedissonConfig {

    @Value("${spring.data.redis.database}")
    private int database;

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private Integer port;

    @Value("${spring.data.redis.password}")
    private String password;

    @Bean(destroyMethod = "shutdown")
    public Redisson redisson() {
        String adsStr = "redis://" + host + ":" + port;
        Config config = new Config();
        SingleServerConfig singleServerConfig = config.useSingleServer();
        singleServerConfig.setAddress(adsStr).setDatabase(database).setConnectionMinimumIdleSize(10);
        if (StrUtil.isNotEmpty(password)) {
            singleServerConfig.setPassword(password);
        }
        return (Redisson) Redisson.create(config);
    }
}
