package com.simple.common.oauth.start.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 * Description: 发送短信验证码请求对象
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
@Schema(description = "发送短信验证码请求对象")
public class CheckSmsRequest {

    @Schema(description = "手机")
    @NotEmpty(message = "手机号不能为空")
    private String phone;

    @Schema(description = "验证码类型")
    @NotEmpty(message = "类型不能为空")
    private String sendType;

    @Schema(description = "验证码")
    @NotEmpty(message = "验证码不能为空")
    private String code;
}
