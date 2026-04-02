package com.simple.oauth.common.dto.sysRole;

import com.simple.oauth.common.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
@Schema(title = "角色信息(sys_role)创建请求参数")
public class CreateSysRoleRequest {

    @Schema(description = "角色名称")
    @NotEmpty(message = "角色名称不能为空")
    private String roleName;

    @Schema(description = "服务（字典）")
    @NotEmpty(message = "服务不能为空")
    private String server;

    @Schema(description = "角色权限字符串")
    @NotEmpty(message = "角色权限字符串不能为空")
    private String roleKey;

    @Schema(description = "角色类型")
    @NotNull(message = "角色类型不能为空")
    private RoleType type;

    @Schema(description = "显示顺序")
    @NotNull(message = "显示顺序不能为空")
    private Integer serial;

    @Schema(description = "角色权限菜单ID")
    private List<String> sysMenuIds;

    @Schema(description = "备注")
    private String remark;
}

