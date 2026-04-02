package com.simple.oauth.common.dto.sysRole;

import com.simple.common.mp.page.PageBase;
import com.simple.oauth.common.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
@Schema(title = "角色信息(sys_role)列表请求参数")
public class FindAllSysRoleRequest extends PageBase {

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "服务（字典）")
    private List<String> server = new ArrayList<>();

    @Schema(description = "角色权限字符串")
    private String roleKey;

    @Schema(description = "角色类型")
    private RoleType type;

    @Schema(description = "用户ID")
    private String userId;

}

