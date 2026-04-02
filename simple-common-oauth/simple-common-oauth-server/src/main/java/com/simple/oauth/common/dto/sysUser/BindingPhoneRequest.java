package com.simple.oauth.common.dto.sysUser;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Data
@Accessors(chain = true)
@Schema(description = "小程序手机号绑定参数对象")
public class BindingPhoneRequest {

    @Schema(description = "手机号")
    @NotEmpty(message = "手机号不能为空")
    private String phone;

    @Schema(description = "验证码")
    @NotEmpty(message = "验证码不能为空")
    private String code;

    @Schema(description = "短信类型")
    @NotEmpty(message = "短信类型不能为空")
    private String sendType;

}
