package com.simple.common.websocket.common.constant;

import com.simple.common.core.exception.AbstractException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * WebSocket异常枚举
 *
 * @author qty
 */
@Getter
@AllArgsConstructor
public enum WebsocketExceptionEnum implements AbstractException {

    /**
     * 握手路径错误
     */
    INVALID_PATH("1001", "握手路径错误"),

    /**
     * 缺少必要参数
     */
    MISSING_PARAM("1002", "缺少必要参数"),

    /**
     * 认证失败
     */
    AUTH_FAILED("1003", "认证失败"),

    /**
     * 消息格式错误
     */
    INVALID_MESSAGE("2001", "消息格式错误"),

    /**
     * 消息过长
     */
    MESSAGE_TOO_LARGE("2002", "消息过长"),

    /**
     * 处理异常
     */
    PROCESS_ERROR("2003", "处理异常"),

    /**
     * 不支持的消息类型
     */
    UNSUPPORTED_FRAME("2004", "不支持的消息类型"),

    /**
     * 连接未认证
     */
    UNAUTHORIZED("2005", "连接未认证"),

    /**
     * 消息为空
     */
    EMPTY_MESSAGE("2006", "消息不能为空"),

    ;

    private final String code;

    private final String message;

}