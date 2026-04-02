package com.simple.oauth.common.dto.sysMenu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
@Schema(title = "菜单权限(sys_menu)明细响应")
public class SysMenuInfoResponse {

    @Schema(description = "菜单id")
    private String id;

    @Schema(description = "客户端ID")
    private String clientId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单唯一标识")
    private String code;

    @Schema(description = "父菜单ID")
    private Integer parentId;

    @Schema(description = "显示顺序")
    private Integer serial;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "路由参数")
    private String query;

    @Schema(description = "是否为外链：1-是，0-否")
    private Integer isFrame;

    @Schema(description = "重定向路径")
    private String redirectUrl;

    @Schema(description = "菜单类型（字典，例如M目录 C菜单 F按钮）")
    private String menuType;

    @Schema(description = "权限标识")
    private String perms;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "扩展")
    private Map<String, Object> reserve;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;
}

