package com.simple.common.auth.client.common.entity.decry;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 * 客户端密钥对象。
 * <p>
 * hsKey 为 32 位 HMAC 密钥：以 Bean Validation 注解声明契约，
 * 在带 @Valid/@Validated 的装配入口（如控制台登记、配置下发校验）即触发校验，
 * 避免长度错误的密钥流转到加解密层才以模糊异常暴露。
 * </p>
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
@Schema(description = "客户端密钥对象")
public class ClientDecry {

    @Schema(description = "客户端（如：xiaoyue_client）")
    private String clientId;

    @Schema(description = "32位密钥字符串")
    @NotBlank(message = "客户端 HMAC 密钥不能为空")
    @Size(min = 32, max = 32, message = "客户端 HMAC 密钥必须为32位")
    private String hsKey;

    @Schema(description = "RSA公钥")
    private String rsaPublic;

    @Schema(description = "RSA私钥")
    private String rsaPrivate;
}
