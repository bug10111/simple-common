package com.simple.common.logs.proto.common.config;

import com.simple.common.logs.proto.common.time.TimeStampProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 日志 proto 模块自动装配
 * <p>
 * 不再对模块根包做整体组件扫描：扫描会把模块内所有构件隐式注册为 Bean，
 * 使用方将 jar 放入 classpath 即启动后台定时线程且无法关闭；
 * 后续模块内新增类也会静默成为所有使用方的 Bean。
 * 现改为显式声明需要的 Bean，并可通过 {@code simple.logs.enabled=false} 关闭。
 * </p>
 *
 * @author qty
 */
@AutoConfiguration
public class LogsProtoConfig {

    /**
     * 秒级时间戳提供者（每秒刷新的缓存时钟）
     */
    @Bean
    @ConditionalOnMissingBean
    public TimeStampProvider timeStampProvider() {
        return new TimeStampProvider();
    }
}
