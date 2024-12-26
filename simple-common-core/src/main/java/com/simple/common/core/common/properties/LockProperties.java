package com.simple.common.core.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA on
 * Description: 缓存要用到的配置
 *
 * @author 兄台丶请冷静
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.lock")
public class LockProperties {

    //缓存是否需要创建包
    private boolean bag = true;

    //存放的默认包名称
    private String defaultBag = "lock";
}
