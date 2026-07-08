package com.simple.common.websocket.common.entity;

import lombok.Data;

/**
 * WebSocket请求实体
 *
 * @param <T> 请求数据类型
 * @author qty
 */
@Data
public class WebSocketRequest<T> {

    /**
     * 请求数据
     */
    private T data;

}