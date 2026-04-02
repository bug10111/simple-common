package com.simple.oauth.common.dto.sysUser;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "用户(sys_user)修改请求参数")
public class RestSysUserRequest {

    @Schema(description = "用户id")
    @NotEmpty(message = "用户id不能为空")
    private String id;

    @Schema(description = "新密码，修改密码时候，旧密码必填")
    @NotEmpty(message = "用户密码不能为空")
    private String password;

}

