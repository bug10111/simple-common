package com.simple.common.websocket.common.manager;

import com.simple.common.websocket.common.entity.WebSocketRequest;
import io.netty.channel.ChannelHandlerContext;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * WebSocket消息监听管理器接口。
 * <p>
 * 用于管理WebSocket消息的监听注册和分发。
 * 配合 {@link com.simple.common.websocket.common.annotation.WebSocketListening} 注解使用,
 * 实现消息的自动监听和处理。
 * 默认实现 {@link com.simple.common.websocket.manager.DefaultWebSocketListeningManager}
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>实时消息推送：订单状态更新、系统通知等</li>
 *   <li>聊天功能：即时通讯消息处理</li>
 *   <li>数据同步：实时数据更新推送</li>
 * </ul>
 *
 * @author qty
 */
public interface WebSocketListeningManager {

    /**
     * 注册监听器方法
     * <p>
     * 将标注了@WebSocketListening注解的方法注册到管理器中。
     * 该方法通常在应用启动时由初始化器自动调用。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 框架内部自动调用,业务代码无需手动注册
     * @Component
     * public class OrderMessageHandler {
     *     @WebSocketListening(type = "order")
     *     public void handleOrder(String message) {
     *         // 处理订单消息
     *     }
     * }
     * }</pre>
     *
     * @param type   消息类型,对应@WebSocketListening注解的type属性
     * @param cliKey 客户端标识,用于区分不同客户端的消息处理
     * @param bean   目标Bean实例
     * @param method 目标方法对象
     */
    void registerMethod(String type, String cliKey, Object bean, Method method);

    /**
     * 调用监听器方法
     * <p>
     * 根据消息类型和客户端标识,找到对应的监听器方法并执行。
     * 当收到WebSocket消息时,框架会自动调用此方法分发消息。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 框架内部调用,业务代码无需手动调用
     * WebSocketRequest request = new WebSocketRequest();
     * request.setType("order");
     * request.setData("{\"orderId\":\"123\"}");
     * 
     * Optional<Object> result = listeningManager.invoke("order", "client123", request);
     * }</pre>
     *
     * @param type    消息类型
     * @param cliKey  客户端标识
     * @param request WebSocket请求对象,包含消息数据
     * @return 调用结果,如果监听器不存在返回Optional.empty()
     */
    Optional<Object> invoke(String type, String cliKey, WebSocketRequest request);
}