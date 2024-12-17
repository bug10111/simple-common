package com.simple.common.core.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA on 2023/11/29/22:44.
 * <p>
 * 服务相关配置
 *
 * @author 兄台丶请冷静
 */
@Getter
@Setter
@Component("applicationProperties")
@ConfigurationProperties(prefix = "spring.application")
public class ApplicationProperties {

    //服务名称
    private String name;

}
