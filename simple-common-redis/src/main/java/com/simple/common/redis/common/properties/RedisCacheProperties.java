package com.simple.common.redis.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: redis缓存配置
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.cache.redis")
public class RedisCacheProperties {

    //缓存是否需要创建包
    private boolean bag = true;

    //存放的默认包名称
    private String defaultBag = "default-cache";

    //接口返回空的时候默认缓存值
    private String noReturnValue = "null";

    //接口没有参数时候的默认key
    private String noParametersKey = "key";

}
