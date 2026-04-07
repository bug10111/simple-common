package com.simple.common.websocket.common.constant;

/**
 * WebSocket常量定义
 *
 * @author qty
 */
public class WebSocketConstant {

    /**
     * 握手类型参数名
     */
    public static final String TYPE = "type";

    /**
     * 客户端标志参数名
     */
    public static final String CLI_KEY = "cli_key";

    /**
     * Token参数名
     */
    public static final String TOKEN = "token";

    /**
     * Channel属性：类型
     */
    public static final String ATTR_TYPE = "ws_type";

    /**
     * Channel属性：客户端标识
     */
    public static final String ATTR_CLI_KEY = "ws_cli_key";

    /**
     * Channel属性：连接时间
     */
    public static final String ATTR_CONNECT_TIME = "ws_connect_time";

    // ========== 错误码定义 ==========

    /**
     * 成功
     */
    public static final int CODE_SUCCESS = 0;

    /**
     * 握手路径错误
     */
    public static final int CODE_INVALID_PATH = 1001;

    /**
     * 缺少必要参数
     */
    public static final int CODE_MISSING_PARAM = 1002;

    /**
     * 认证失败
     */
    public static final int CODE_AUTH_FAILED = 1003;

    /**
     * 消息格式错误
     */
    public static final int CODE_INVALID_MESSAGE = 2001;

    /**
     * 消息过长
     */
    public static final int CODE_MESSAGE_TOO_LARGE = 2002;

    /**
     * 处理异常
     */
    public static final int CODE_PROCESS_ERROR = 2003;

    /**
     * 不支持的消息类型
     */
    public static final int CODE_UNSUPPORTED_FRAME = 2004;

}
