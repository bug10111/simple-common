package com.simple.common.websocket.common.annotation;

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
     * 理论上一个类型在同一个端只能有一个数据通道
     */
    String type();

    /**
     * 用于区分相同类型时候，不同端的数据
     */
    String cliKey();

}
