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
    public static final String CLI_KEY = "cliKey";

    /**
     * Token参数名
     */
    public static final String TOKEN = "token";

    /**
     * 编解码方式参数名（json / proto），缺省 json
     */
    public static final String CODEC = "codec";

    /**
     * 编解码方式：JSON
     */
    public static final String CODEC_JSON = "json";

    /**
     * 编解码方式：Protobuf
     */
    public static final String CODEC_PROTO = "proto";

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

    /**
     * Channel属性：编解码方式
     */
    public static final String ATTR_CODEC = "ws_codec";

}
