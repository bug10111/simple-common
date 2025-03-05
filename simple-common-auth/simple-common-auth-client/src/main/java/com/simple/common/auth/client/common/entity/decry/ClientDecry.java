package com.simple.common.auth.client.common.entity.decry;

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
@Schema(description = "客户端密钥对象")
public class ClientDecry {

    @Schema(description = "客户端（如：xiaoyue_client）")
    private String clientId;

    @Schema(description = "32位密钥字符串")
    private String hsKey;

    @Schema(description = "RSA公钥")
    private String rsaPublic;

    @Schema(description = "RSA私钥")
    private String rsaPrivate;
}
