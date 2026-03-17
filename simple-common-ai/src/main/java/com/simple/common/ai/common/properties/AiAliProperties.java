package com.simple.common.ai.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 阿里云ai配置
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.ai.ali")
public class AiAliProperties {

    //秘钥
    private String apiKey = "sk-f07924faa49e475ab791ab3399cb1bee";

    //appid
    private String appId = "685ac22036b44005ad25e44f00ae61bd";

    //是否开启深入思考
    private boolean enableThinking = false;

}
