package com.simple.oauth.common.dto.sysRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "角色信息(sys_role)修改请求参数")
public class UpdateSysRoleRequest extends CreateSysRoleRequest {

    @Schema(description = "角色id")
    @NotEmpty(message = "角色id不能为空")
    private String id;

}

