package com.simple.oauth.common.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
@Schema(description = "秘钥返回")
public class ApiSysClientDetailsResponse {

    @Schema(description = "客户端（如：xiaoyue_client）")
    private String clientId;

    @Schema(description = "32位密钥字符串")
    private String hsKey;

    @Schema(description = "RSA公钥")
    private String rsaPublic;

    @Schema(description = "RSA私钥")
    private String rsaPrivate;

}
