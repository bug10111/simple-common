package com.simple.common.oauth.start.common.event.sysMenu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.eventbus.common.annotation.Event;
import com.simple.common.eventbus.common.constants.EventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 菜单权限(sys_menu)创建事件
 *
 * @author qty
 */
@Data
@Event(targets = EventConstant.TARGET_ALL_X)
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class SysMenuCreatedEvent {

    @Schema(description = "菜单id")
    private String id;

    @Schema(description = "客户端ID")
    private String clientId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单唯一标识")
    private String code;

    @Schema(description = "权限标识")
    private String perms;

    @Schema(description = "缓存到admin 的key")
    private String authKey;
}

