package com.simple.oauth.common.dto.sysUserLoginKey;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "用户登录标志(sys_user_login_key)创建请求参数")
public class CreateSysUserLoginKeyRequest {

    @Schema(description = "用户Id")
    @NotEmpty(message = "用户Id不能为空")
    private String userId;

    @Schema(description = "第三方登录标志")
    @NotEmpty(message = "第三方登录标志不能为空")
    private String loginKey;
}

