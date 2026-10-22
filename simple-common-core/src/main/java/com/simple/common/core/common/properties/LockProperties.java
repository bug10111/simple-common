package com.simple.common.core.common.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Created by IntelliJ IDEA
 * Description: 分布式锁要用到的配置（由 AbsLockService / 限流管理器消费）
 *
 * @author qty
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "simple.lock")
public class LockProperties {

    //锁key是否需要创建命名空间前缀（bag）
    private boolean bag = true;

    //锁key默认命名空间前缀（空值会拼出 ":method:key" 之类畸形键，启动期快速失败）
    @NotBlank(message = "simple.lock.default-bag 不能为空")
    private String defaultBag = "lock";
}
