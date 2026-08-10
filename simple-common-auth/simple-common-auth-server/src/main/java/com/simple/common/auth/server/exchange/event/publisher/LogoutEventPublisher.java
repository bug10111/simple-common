package com.simple.common.auth.server.exchange.event.publisher;

import com.simple.common.auth.client.exchange.event.event.UserLoggedOutEvent;
import com.simple.common.eventbus.common.service.EventBusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 用户退出登录事件发布器。
 * <p>
 * 专职负责构造并发布用户退出登录事件，广播到所有客户端清除本地缓存。
 * 业务 service 无需感知事件构造细节，仅调用发布器即可。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class LogoutEventPublisher {

    @Autowired
    private EventBusService eventBusService;

    /**
     * 发布用户退出登录事件，广播到所有客户端清除本地 Token 缓存。
     * <p>
     * 事件通过 RabbitMQ Fanout 交换机广播到所有已绑定的服务，
     * 各客户端的 oauth-start 消费后清除本地 Redis 中该用户的 Token 数据，
     * 实现"一处退出、全部下线"。
     * </p>
     *
     * @param userId 退出登录的用户ID
     */
    public void publishLogout(String userId) {
        UserLoggedOutEvent event = new UserLoggedOutEvent();
        event.setUserId(userId);

        eventBusService.push(event);
        log.debug("已发布用户退出登录事件，userId: {}", userId);
    }
}
