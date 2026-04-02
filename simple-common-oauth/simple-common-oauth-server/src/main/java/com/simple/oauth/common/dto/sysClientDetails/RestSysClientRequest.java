package com.simple.oauth.common.dto.sysClientDetails;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "客户端修改请求参数")
public class RestSysClientRequest {

    @Schema(description = "用户id")
    @NotEmpty(message = "用户id不能为空")
    private String id;

    @Schema(description = "旧密码")
    @NotEmpty(message = "旧密码不能为空")
    private String passwordOld;

    @Schema(description = "新密码")
    @NotEmpty(message = "新密码不能为空")
    private String password;

}

