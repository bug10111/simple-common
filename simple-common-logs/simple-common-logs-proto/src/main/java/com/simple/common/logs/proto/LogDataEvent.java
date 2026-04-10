package com.simple.common.logs.proto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 日志数据事件
 *
 * @author Admin
 */
@Data
public class LogDataEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    private String id;

    /**
     * 追踪 ID
     */
    private String traceId;

    /**
     * 操作 IP
     */
    private String operIp;

    /**
     * 方法名
     */
    private String method;

    /**
     * 操作 URL
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
     * 用户 ID
     */
    private long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 部门 ID
     */
    private long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 操作类型
     */
    private String operType;

    /**
     * 状态
     */
    private int status;

    /**
     * 耗时（毫秒）
     */
    private long costTime;

    /**
     * 客户端 ID
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
     * 标题
     */
    private String title;

    /**
     * 错误消息
     */
    private String errorMsg;

    /**
     * 昵称
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
     * 模块
     */
    private String module;
}
