package com.simple.common.core.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 *  Description: 阿里配置类
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.ali")
public class AlibabaProperties {
    private String accessKeyId;

    private String accessKeySecret;
}
