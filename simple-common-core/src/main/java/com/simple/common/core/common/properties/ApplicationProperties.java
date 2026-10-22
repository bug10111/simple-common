package com.simple.common.core.common.properties;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 服务相关配置
 *
 * @author qty
 */
@Getter
@Setter
@Component("applicationProperties")
@ConfigurationProperties(prefix = "spring.application")
public class ApplicationProperties {

    //服务名称
    private String name;

    /**
     * 启动期快速失败：服务名缺失时，下游消费点（MQ命名、鉴权clientId派生等）只会在运行期
     * 以含混NPE或畸形命名暴露，这里在装配完成后立即校验并给出明确报错
     */
    @PostConstruct
    public void validate() {
        if (StrUtil.isBlank(name)) {
            throw new IllegalStateException("spring.application.name 未配置，服务名称不能为空");
        }
    }
}
