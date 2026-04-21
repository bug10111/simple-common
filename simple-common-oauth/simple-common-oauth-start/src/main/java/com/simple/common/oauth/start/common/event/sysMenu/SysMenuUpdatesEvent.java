package com.simple.common.oauth.start.common.event.sysMenu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.eventbus.common.annotation.Event;
import com.simple.common.eventbus.common.constants.EventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * 菜单权限(sys_menu)删除事件
 *
 * @author qty
 */
@Data
@Event(targets = EventConstant.TARGET_ALL_X)
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class SysMenuUpdatesEvent {

    @Schema(description = "缓存到admin 的key")
    private String authKey;

    @Schema(description = "缓存")
    Map<Object, Object> collect;

}

