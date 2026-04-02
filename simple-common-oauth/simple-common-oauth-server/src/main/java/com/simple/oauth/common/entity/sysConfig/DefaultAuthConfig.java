package com.simple.oauth.common.entity.sysConfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Data
@Accessors(chain = true)
@Schema(description = "oauth的配置实体")
public class DefaultAuthConfig {

    @Schema(description = "jwt密钥")
    private String jwtKey;
}
