package com.simple.oauth.common.dto.sysUser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
@Schema(title = "用户(sys_user)明细响应")
public class SysUserByRoleKeyResponse {

    @Schema(description = "用户id")
    private String id;

    @Schema(description = "用户账号")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

}

