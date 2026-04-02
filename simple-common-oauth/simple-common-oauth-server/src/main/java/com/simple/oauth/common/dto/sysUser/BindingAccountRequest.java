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
@Schema(description = "小程序账号绑定参数对象")
public class BindingAccountRequest {

    @Schema(description = "账号")
    @NotEmpty(message = "账号不能为空")
    private String username;

    @Schema(description = "密码")
    @NotEmpty(message = "密码不能为空")
    private String password;
}
