package com.simple.common.websocket.common.annotation;

import com.simple.common.websocket.common.constant.WebSocketConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created with IntelliJ IDEA
 * Description: WebSocket消息分发，标记方法后会接收到对应的消息
 *
 * @author qty
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface WebSocketListening {

    /**
     * 表示socket类型，用于区分不同通讯。
     * 理论上一个类型在同一个端只能有一个数据通道。
     * <p>
     * 契约：type 不可为空白（注解本身无法声明约束，注册路径
     * {@code WebSocketListeningInit} 对空白 type 快速失败），
     * 且同一 {@code (type, cliKey)} 重复注册时后注册者覆盖先注册者（覆盖时记录告警日志）。
     * </p>
     */
    String type();

    /**
     * 用于区分相同类型时候，不同端的数据。
     * <p>
     * 契约：不可为 null（缺省 {@value WebSocketConstant#DEFAULT_CLI_KEY}）。
     * 分发时先按 cliKey 精确匹配，匹配不到则回退到以 {@value WebSocketConstant#DEFAULT_CLI_KEY}
     * 注册的通配处理器——因此 {@value WebSocketConstant#DEFAULT_CLI_KEY} 在注册侧是通配标记，
     * 不要用空白值注册（空白 cliKey 无法通过通配回退被路由到）。
     * </p>
     */
    String cliKey() default WebSocketConstant.DEFAULT_CLI_KEY;

}
