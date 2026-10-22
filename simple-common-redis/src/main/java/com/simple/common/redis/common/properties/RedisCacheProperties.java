package com.simple.common.redis.common.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Created by IntelliJ IDEA
 * Description: redis缓存配置
 * <p>
 * 三个 Key 相关配置项均直接参与缓存 Key 拼接（前缀/空值占位/无参占位），
 * 置空会产生形如 {@code ":key"} 或以 {@code "&"} 结尾的畸形 Key，引发键冲突与排查困难，
 * 故声明为非空校验，配置错误在启动期快速失败。
 *
 * @author qty
 */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "simple.cache.redis")
public class RedisCacheProperties {

    //缓存是否需要创建包
    private boolean bag = true;

    //存放的默认包名称
    @NotBlank(message = "simple.cache.redis.default-bag 不能为空白")
    private String defaultBag = "default-cache";

    //接口返回空的时候默认缓存值
    @NotBlank(message = "simple.cache.redis.no-return-value 不能为空白")
    private String noReturnValue = "null";

    //接口没有参数时候的默认key
    @NotBlank(message = "simple.cache.redis.no-parameters-key 不能为空白")
    private String noParametersKey = "key";

}
