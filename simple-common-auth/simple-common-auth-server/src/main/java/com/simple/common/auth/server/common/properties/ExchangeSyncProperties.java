package com.simple.common.auth.server.common.properties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 变更投递引擎配置
 * <p>
 * 投递-ACK-重投链路的可配参数：失败重投间隔、重投次数上限、回调超时。
 * 数值项均以基本类型承载（装配期缺省值兜底、杜绝拆箱 NPE），并在装配期完成正数校验——
 * 非法配置在启动期以可诊断异常失败，而不是在投递期表现为零/负超时或重试静默失效。
 * </p>
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.auth.exchange")
public class ExchangeSyncProperties {

    /**
     * 投递失败重投间隔（秒）
     */
    private int retryIntervalSeconds = 30;

    /**
     * 投递失败重投次数上限（首次投递后最多再投 N 次，超限落失败记录）
     */
    private int maxRetries = 5;

    /**
     * 单实例回调超时（秒）
     */
    private int callbackTimeoutSeconds = 10;

    /**
     * 装配期校验：非法配置启动即失败，不在投递期才暴露
     */
    @PostConstruct
    public void validate() {
        if (retryIntervalSeconds <= 0) {
            throw new IllegalStateException("simple.auth.exchange.retry-interval-seconds 必须为正整数（秒），当前值: " + retryIntervalSeconds);
        }
        if (maxRetries < 0) {
            throw new IllegalStateException("simple.auth.exchange.max-retries 不能为负数，当前值: " + maxRetries);
        }
        if (callbackTimeoutSeconds <= 0 || callbackTimeoutSeconds > 86400) {
            throw new IllegalStateException("simple.auth.exchange.callback-timeout-seconds 必须为 1~86400 的整数（秒），当前值: " + callbackTimeoutSeconds);
        }
    }

}
