package com.simple.common.eventbus.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 事件配置类
 *
 * @author 兄台丶请冷静
 */
@Getter
@Setter
@Component("eventProperties")
@ConfigurationProperties(prefix = "simple.event")
public class EventProperties {
    private String type = "mq";

    private String concurrency = "1";
}
