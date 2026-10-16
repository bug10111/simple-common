package com.simple.common.auth.server.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 变更投递引擎配置
 * <p>
 * 投递-ACK-重投链路的可配参数：失败重投间隔、重投次数上限、
 * 回调超时、实例注册超期判定（0=不启用超期过滤）。
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
    private Integer retryIntervalSeconds = 30;

    /**
     * 投递失败重投次数上限（首次投递后最多再投 N 次，超限落失败记录）
     */
    private Integer maxRetries = 5;

    /**
     * 单实例回调超时（秒）
     */
    private Integer callbackTimeoutSeconds = 10;

    /**
     * 实例注册超期（小时；实例超过该时长未重注册则不再参与投递；0=不启用超期过滤）
     */
    private Integer instanceExpireHours = 0;

}
