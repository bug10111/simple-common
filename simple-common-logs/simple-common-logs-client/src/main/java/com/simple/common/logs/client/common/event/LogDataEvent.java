package com.simple.common.logs.client.common.event;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 日志数据事件
 * 用于传输日志数据
 *
 * @author qty
 */
@Data
public class LogDataEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    private String id;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作类型
     */
    private String operType;

    /**
     * 操作IP
     */
    private String operIp;

    /**
     * 操作方法
     */
    private String method;

    /**
     * 操作URL
     */
    private String operUrl;

    /**
     * 操作名称
     */
    private String operName;

    /**
     * 操作参数
     */
    private String operParam;

    /**
     * 操作结果
     */
    private String operResult;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 操作时间
     */
    private LocalDateTime operTime;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作用户名
     */
    private String userName;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求耗时（毫秒）
     */
    private Long costTime;

    /**
     * 客户端标识
     */
    private String clientId;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * 操作标题
     */
    private String title;

    /**
     * 操作状态（0成功 1失败）
     */
    private Integer status;

    /**
     * 错误消息
     */
    private String errorMsg;

    /**
     * 错误数据
     */
    private String errorData;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 请求时间
     */
    private LocalDateTime requestTime;

    /**
     * 操作地点
     */
    private String operLocation;

    /**
     * 创建默认的日志事件
     */
    public static LogDataEvent createDefault() {
        LogDataEvent event = new LogDataEvent();
        event.setCreateTime(LocalDateTime.now());
        event.setOperTime(LocalDateTime.now());
        return event;
    }
}