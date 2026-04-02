package com.simple.oauth.common.dto.sysMenu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "菜单权限(sys_menu)修改请求参数")
public class UpdateSysMenuRequest extends CreateSysMenuRequest {

    @Schema(description = "菜单id")
    @NotEmpty(message = "菜单id不能为空")
    private String id;

}

