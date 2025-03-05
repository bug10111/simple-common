package com.simple.common.logs.server.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 日志配置类
 *
 * @author 兄台丶请冷静
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.logs")
public class LogsServerProperties {

    //一次提交多少条数据
    private int saveSum = 2000;

    //多长时间提交一次
    private int time = 3;

}
