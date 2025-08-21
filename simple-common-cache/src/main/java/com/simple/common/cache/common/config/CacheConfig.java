package com.simple.common.cache.common.config;

import com.simple.common.cache.common.factory.LocalCacheFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Configuration
@ComponentScan(basePackages = { "com.simple.common.cache" })
public class CacheConfig {

    @Bean
    @SuppressWarnings("rawtypes")
    public LocalCacheFactory<?, ?> localCacheFactory(){
        return new LocalCacheFactory();
    }

}
