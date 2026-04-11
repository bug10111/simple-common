package com.simple.common.logs.server.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.simple.common.logs.client.common.event.LogDataEvent;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 *
 * @author qty
 */
@Data
@TableName("sys_operation_logs")
public class SysOperationLogs implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 追踪ID
     */
    @TableField("trace_id")
    private String traceId;

    /**
     * 方法名称/操作标题
     */
    @TableField("title")
    private String title;

    /**
     * 请求方式
     */
    @TableField("method")
    private String method;

    /**
     * 请求URL
     */
    @TableField("oper_url")
    private String operUrl;

    /**
     * 主机地址
     */
    @TableField("oper_ip")
    private String operIp;

    /**
     * 操作地点
     */
    @TableField("oper_location")
    private String operLocation;

    /**
     * 操作人员id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 用户名
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 操作名称
     */
    @TableField("oper_name")
    private String operName;

    /**
     * 请求参数
     */
    @TableField("oper_param")
    private String operParam;

    /**
     * 操作状态（0成功 1失败）
     */
    @TableField("status")
    private Integer status;

    /**
     * 错误消息
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 异常信息（堆栈）
     */
    @TableField("error_data")
    private String errorData;

    /**
     * 接口耗时（毫秒），存储为本地时间
     */
    @TableField("request_time")
    private LocalDateTime requestTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 从LogDataEvent转换
     */
    public static SysOperationLogs fromLogDataEvent(LogDataEvent event) {
        SysOperationLogs logs = new SysOperationLogs();
        // 设置 TraceId
        logs.setTraceId(event.getTraceId());
        // 设置标题
        logs.setTitle(event.getTitle());
        // 设置操作方法
        logs.setMethod(event.getMethod());
        // 设置操作URL
        logs.setOperUrl(event.getOperUrl());
        // 设置操作IP
        logs.setOperIp(event.getOperIp());
        // 设置操作地点
        logs.setOperLocation(event.getOperLocation());
        // 设置操作名称
        logs.setOperName(event.getOperName());
        // 设置用户ID（LogDataEvent中userId为String类型）
        if (event.getUserId() != null && !event.getUserId().isEmpty()) {
            try {
                logs.setUserId(Long.parseLong(event.getUserId()));
            } catch (NumberFormatException e) {
                logs.setUserId(null);
            }
        }
        // 设置用户昵称
        logs.setNickname(event.getNickname());
        // 设置操作参数
        logs.setOperParam(event.getOperParam());
        // 设置操作状态
        logs.setStatus(event.getStatus());
        // 设置错误消息
        logs.setErrorMsg(event.getErrorMsg());
        // 设置错误详情
        logs.setErrorData(event.getErrorData());
        // 设置请求时间（LogDataEvent中requestTime为Long类型，表示时间戳）
        if (event.getRequestTime() != null) {
            logs.setRequestTime(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(event.getRequestTime()), java.time.ZoneId.systemDefault()));
        }
        // 设置创建时间
        logs.setCreateTime(event.getCreateTime() != null ? event.getCreateTime().toLocalDateTime() : LocalDateTime.now());
        return logs;
    }
}