package com.simple.oauth.event;

import com.simple.common.eventbus.common.annotation.EventHandler;
import com.simple.common.eventbus.common.constants.EventConstant;
import com.simple.oauth.common.event.sysMenu.SysMenuCreatedEvent;
import com.simple.oauth.common.event.sysMenu.SysMenuDeletedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 菜单事件实现
 *
 * @author qty
 */
@Component
public class SysMenuEventHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @EventHandler(applicationName = EventConstant.TARGET_ALL_X)
    public void create(SysMenuCreatedEvent event) {
        redisTemplate.opsForHash().put(event.getAuthKey(), event.getPerms(), event.getPerms());
    }

    @EventHandler(applicationName = EventConstant.TARGET_ALL_X)
    public void deleted(SysMenuDeletedEvent event) {
        redisTemplate.opsForHash().delete(event.getAuthKey(), event.getPerms());
    }

}
