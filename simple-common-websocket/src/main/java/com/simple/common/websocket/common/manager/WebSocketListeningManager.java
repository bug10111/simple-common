package com.simple.common.websocket.common.manager;

import com.simple.common.websocket.common.entity.WebSocketRequest;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * WebSocket消息监听管理器接口。
 * <p>
 * 用于管理WebSocket消息的监听注册和分发。
 * 配合 {@link com.simple.common.websocket.common.annotation.WebSocketListening} 注解使用，
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
     * 注册监听方法。
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
     *     public void handleOrder(WebSocketRequest<OrderDto> request) {
     *         OrderDto data = request.getData();
     *     }
     * }
     * }</pre>
     *
     * <p>实现契约：</p>
     * <ul>
     *   <li>type / cliKey / bean / method / dataType 均不可为 null（实现应快速失败）；
     *       无法解析泛型时 dataType 传 {@link Object}（表示不做类型转换）</li>
     *   <li>实现必须保证注册与 {@link #invoke} 的并发安全（注册发生在启动阶段，
     *       invoke 运行在 Netty worker 线程）</li>
     *   <li>同一 {@code (type, cliKey)} 重复注册时以后注册者为准（覆盖），覆盖行为应记录日志</li>
     * </ul>
     *
     * @param type     消息类型,对应@WebSocketListening注解的type属性,不可为空白
     * @param cliKey   客户端标识,用于区分不同客户端的消息处理,不可为null。
     *                 分发时先按cliKey精确匹配,匹配不到则回退到以
     *                 {@value WebSocketConstant#DEFAULT_CLI_KEY}注册的通配处理器
     * @param bean     目标Bean实例
     * @param method   目标方法对象
     * @param dataType WebSocketRequest泛型中data的实际类型,不可为null;
     *                 无法解析泛型时传{@link Object}表示不做类型转换
     */
    void registerMethod(String type, String cliKey, Object bean, Method method, Class<?> dataType);

    /**
     * 调用监听方法
     * <p>
     * 根据消息类型和客户端标识,找到对应的监听方法并执行。
     * 当收到WebSocket消息时,框架会自动调用此方法分发消息。
     * 内部会根据注册时记录的dataType自动将data字段反序列化为对应类型。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 框架内部调用,业务代码无需手动调用
     * WebSocketRequest<Object> request = new WebSocketRequest<>();
     * request.setData(jsonObject);
     *
     * Optional<Object> result = listeningManager.invoke("order", "client123", request);
     * }</pre>
     *
     * <p>契约说明：</p>
     * <ul>
     *   <li>cliKey 为 null 时按无精确匹配处理（直接走通配回退），实现不得抛 NPE</li>
     *   <li>监听方法抛出的异常会向上传播（原始异常，非反射包装），调用方不应静默吞掉</li>
     * </ul>
     *
     * @param type    消息类型
     * @param cliKey  客户端标识,可为null（null 视为无精确匹配）
     * @param request WebSocket请求对象,包含消息数据
     * @return 监听器返回值;监听器不存在或监听方法返回void/null时均为Optional.empty()。
     *         注意:调用方无法据此区分"未找到监听器"与"已处理但无返回值";
     *         需要"未处理"感知时请通过日志或其他途径观察
     */
    Optional<Object> invoke(String type, String cliKey, WebSocketRequest<?> request);
}
