package com.simple.common.logs.client.common.constant;

/**
 * 日志常量
 *
 * @author qty
 */
public class LogConstant {

    /**
     * TraceId 请求头名称
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * TraceId 请求属性名（独立于 HTTP 头命名空间，避免与其他组件的属性键冲突）
     */
    public static final String TRACE_ID_ATTR = "logTraceId";

    /**
     * 请求开始时间属性名
     */
    public static final String START_TIME = "startTime";

}