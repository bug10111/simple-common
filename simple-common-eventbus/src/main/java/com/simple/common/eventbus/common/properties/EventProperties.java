package com.simple.common.eventbus.common.properties;

import com.simple.common.eventbus.common.constants.EventConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Created with IntelliJ IDEA
 * Description: 事件配置类
 * <p>启动期即校验：type 只允许 sync|mq，concurrency 只允许正整数字符串，
 * 配置错误在装配阶段快速失败，避免运行期静默退化为无事件能力或监听器创建时报错。</p>
 *
 * @author qty
 */
@Getter
@Setter
@Validated
@Component("eventProperties")
@ConfigurationProperties(prefix = "simple.event")
public class EventProperties {

    /**
     * 事件模式：sync（发布方同线程执行处理器）或 mq（经 RabbitMQ 消费线程执行）
     */
    @NotBlank(message = "simple.event.type 不能为空，仅支持 sync|mq")
    @Pattern(regexp = "sync|mq", message = "simple.event.type 仅支持 sync|mq")
    private String type = EventConstant.EVENT_TYPE_MQ;

    /**
     * MQ 监听器并发数（须为正整数的字符串形式，供 @RabbitListener concurrency 使用，支持如 1 或 1-3 区间写法）
     */
    @NotBlank(message = "simple.event.concurrency 不能为空，仅支持正整数（或 n-m 区间）")
    @Pattern(regexp = "[1-9][0-9]*(-[1-9][0-9]*)?", message = "simple.event.concurrency 仅支持正整数（或 n-m 区间，如 1 或 1-3）")
    private String concurrency = "1";
}
