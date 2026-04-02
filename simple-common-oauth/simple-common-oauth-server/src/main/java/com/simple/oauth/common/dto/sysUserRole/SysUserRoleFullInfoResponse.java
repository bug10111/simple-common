package com.simple.oauth.common.dto.sysUserRole;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
@Schema(title = "用户和角色关联(sys_user_role)明细响应")
public class SysUserRoleFullInfoResponse {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "角色ID")
    private String roleId;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;
}

