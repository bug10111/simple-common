package com.simple.common.auth.client.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.eventbus.common.annotation.Event;
import com.simple.common.eventbus.common.constants.EventConstant;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户退出登录事件。
 * <p>
 * 当用户在任一客户端退出登录时，OAuth Server 发布此事件（广播到所有系统）。
 * 各客户端的 oauth-start 消费此事件，清除本地 Redis 中该用户的 Token 数据，
 * 实现"一处退出、全部下线"。
 * </p>
 *
 * @author qty
 */
@Data
@Event( targets = EventConstant.TARGET_ALL_X)
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class UserLoggedOutEvent {

    /**
     * 退出登录的用户ID
     */
    private String userId;

    /**
     * 退出登录的用户名（用于审计日志）
     */
    private String username;

}
