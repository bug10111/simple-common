package com.simple.oauth.common.dto.sysUserLoginKey;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "用户登录标志(sys_user_login_key)修改请求参数")
public class UpdateSysUserLoginKeyRequest extends CreateSysUserLoginKeyRequest {

    @Schema(description = "主键")
    @NotEmpty(message = "主键不能为空")
    private String id;

}

