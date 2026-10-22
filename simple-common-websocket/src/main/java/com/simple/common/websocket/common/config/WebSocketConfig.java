package com.simple.common.websocket.common.config;

import com.simple.common.websocket.common.manager.CheckWebSocketManager;
import com.simple.common.websocket.common.properties.WebSocketProperties;
import com.simple.common.websocket.init.WebSocketChannelCleanTask;
import com.simple.common.websocket.init.WebSocketListeningInit;
import com.simple.common.websocket.init.WebSocketStartInit;
import com.simple.common.websocket.manager.DefaultCheckWebSocketManager;
import com.simple.common.websocket.manager.DefaultWebSocketListeningManager;
import com.simple.common.websocket.manager.DefaultWebSocketSyncManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * WebSocket 自动装配入口。
 * <p>
 * 以 {@link AutoConfiguration} + {@link Import} 显式声明本模块全部 Bean，
 * 不再使用 {@code @ComponentScan}：组件扫描运行在自动装配管线之外，
 * 会导致宿主应用无法通过 {@code spring.autoconfigure.exclude} 排除、
 * 且与宿主自身组件扫描产生重复/冲突 Bean。需要替换的默认实现
 * （如 {@link DefaultCheckWebSocketManager}）以
 * {@code @ConditionalOnMissingBean} 注册，业务子类 Bean 可无缝覆盖。
 * </p>
 * <p>
 * 条件装配：设置 {@code simple.websocket.enabled=false} 可整体关闭 WebSocket 服务，
 * 缺省开启（保持既有默认行为）。
 * </p>
 *
 * @author qty
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "simple.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({
        WebSocketProperties.class,
        WebSocketStartInit.class,
        WebSocketListeningInit.class,
        WebSocketChannelCleanTask.class,
        DefaultWebSocketListeningManager.class,
        DefaultWebSocketSyncManager.class
})
public class WebSocketConfig {

    /**
     * 默认鉴权校验器：仅当上下文不存在 CheckWebSocketManager Bean 时注册。
     * 业务方注册自定义实现（含 DefaultCheckWebSocketManager 子类）即可无缝替换默认实现，
     * 避免"默认实现 + 业务子类"双 Bean 导致的 NoUniqueBeanDefinitionException。
     */
    @Bean
    @ConditionalOnMissingBean(CheckWebSocketManager.class)
    public DefaultCheckWebSocketManager defaultCheckWebSocketManager(WebSocketProperties properties) {
        return new DefaultCheckWebSocketManager(properties);
    }
}
