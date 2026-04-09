package com.simple.common.logs.client.common.constant;

/**
 * 日志常量
 *
 * @author qty
 */
public class LogConstant {

    /**
     * 请求开始时间属性名
     */
    public static final String START_TIME = "log_start_time";

    /**
     * TCP帧头
     */
    public static final int FRAME_HEAD = 0xAA55;

    /**
     * TCP帧尾
     */
    public static final int FRAME_TAIL = 0x55AA;

    /**
     * 日志请求成功状态码
     */
    public static final int STATUS_SUCCESS = 200;

    /**
     * 日志请求失败状态码
     */
    public static final int STATUS_ERROR = 500;

}