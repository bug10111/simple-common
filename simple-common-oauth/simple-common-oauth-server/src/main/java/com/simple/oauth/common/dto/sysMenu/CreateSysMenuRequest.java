package com.simple.oauth.common.dto.sysMenu;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
@Schema(title = "菜单权限(sys_menu)创建请求参数")
public class CreateSysMenuRequest {

    @Schema(description = "客户端ID")
    @NotEmpty(message = "客户端ID不能为空")
    private String clientId;

    @Schema(description = "菜单名称")
    @NotEmpty(message = "菜单名称不能为空")
    private String menuName;

    @Schema(description = "菜单唯一标识")
    @NotEmpty(message = "菜单唯一标识不能为空")
    private String code;

    @Schema(description = "父菜单ID，第一级不填")
    private String parentId;

    @Schema(description = "显示顺序")
    @NotNull(message = "显示顺序不能为空")
    private Integer serial;

    @Schema(description = "路由地址")
    @NotEmpty(message = "路由地址不能为空")
    private String path;

    @Schema(description = "组件路径")
    @NotEmpty(message = "组件路径不能为空")
    private String component;

    @Schema(description = "路由参数")
    @NotEmpty(message = "路由参数不能为空")
    private String query;

    @Schema(description = "是否为外链：1-是，0-否")
    @NotNull(message = "是否为外链：1-是，0-否不能为空")
    private Integer isFrame;

    @Schema(description = "重定向路径")
    private String redirectUrl;

    @Schema(description = "菜单类型（字典，例如M目录 C菜单 F按钮）")
    @NotEmpty(message = "菜单类型（字典，例如M目录 C菜单 F按钮）不能为空")
    private String menuType;

    @Schema(description = "权限标识（服务端接口标志）")
    @NotEmpty(message = "权限标识不能为空")
    private String perms;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "扩展")
    private Map<String, Object> reserve;

    @Schema(description = "备注")
    private String remark;
}

