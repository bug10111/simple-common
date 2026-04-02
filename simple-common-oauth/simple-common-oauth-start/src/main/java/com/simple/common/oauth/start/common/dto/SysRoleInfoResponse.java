package com.simple.common.oauth.start.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.oauth.start.common.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
@Schema(title = "角色信息(sys_role)明细响应")
public class SysRoleInfoResponse {

    @Schema(description = "角色id")
    private String id;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色权限字符串")
    private String roleKey;

    @Schema(description = "角色类型")
    private RoleType type;

    @Schema(description = "显示顺序")
    private Integer serial;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;
}
