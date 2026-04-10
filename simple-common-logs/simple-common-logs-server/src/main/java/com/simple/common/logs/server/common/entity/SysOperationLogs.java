package com.simple.common.logs.server.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.simple.common.logs.proto.LogDataEvent;
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
     * 操作模块
     */
    @TableField("module")
    private String module;

    /**
     * 操作类型
     */
    @TableField("oper_type")
    private String operType;

    /**
     * 操作IP
     */
    @TableField("oper_ip")
    private String operIp;

    /**
     * 操作方法
     */
    @TableField("method")
    private String method;

    /**
     * 操作URL
     */
    @TableField("oper_url")
    private String operUrl;

    /**
     * 操作名称
     */
    @TableField("oper_name")
    private String operName;

    /**
     * 操作参数
     */
    @TableField("oper_param")
    private String operParam;

    /**
     * 操作结果
     */
    @TableField("oper_result")
    private String operResult;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 操作时间
     */
    @TableField("oper_time")
    private LocalDateTime operTime;

    /**
     * 操作用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 操作用户名
     */
    @TableField("user_name")
    private String userName;

    /**
     * 部门ID
     */
    @TableField("dept_id")
    private Long deptId;

    /**
     * 部门名称
     */
    @TableField("dept_name")
    private String deptName;

    /**
     * 请求方法
     */
    @TableField("request_method")
    private String requestMethod;

    /**
     * 请求耗时（毫秒）
     */
    @TableField("cost_time")
    private Long costTime;

    /**
     * 客户端标识
     */
    @TableField("client_id")
    private String clientId;

    /**
     * 业务类型
     */
    @TableField("business_type")
    private String businessType;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 追踪ID
     */
    @TableField("trace_id")
    private String traceId;

    /**
     * 操作标题
     */
    @TableField("title")
    private String title;

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
     * 用户昵称
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 请求时间
     */
    @TableField("request_time")
    private LocalDateTime requestTime;

    /**
     * 操作地点
     */
    @TableField("oper_location")
    private String operLocation;

    /**
     * 从LogDataEvent转换
     */
    public static SysOperationLogs fromLogDataEvent(LogDataEvent event) {
        SysOperationLogs logs = new SysOperationLogs();
        logs.setId(event.getId());
        logs.setModule(event.getModule());
        logs.setOperType(event.getOperType());
        logs.setOperIp(event.getOperIp());
        logs.setMethod(event.getMethod());
        logs.setOperUrl(event.getOperUrl());
        logs.setOperName(event.getOperName());
        logs.setOperParam(event.getOperParam());
        logs.setOperResult(event.getOperResult());
        logs.setErrorMessage(event.getErrorMessage());
        logs.setOperTime(event.getOperTime());
        logs.setUserId(event.getUserId());
        logs.setUserName(event.getUserName());
        logs.setDeptId(event.getDeptId());
        logs.setDeptName(event.getDeptName());
        logs.setRequestMethod(event.getRequestMethod());
        logs.setCostTime(event.getCostTime());
        logs.setClientId(event.getClientId());
        logs.setBusinessType(event.getBusinessType());
        logs.setCreateTime(event.getCreateTime() != null ? event.getCreateTime() : LocalDateTime.now());
        logs.setTraceId(event.getTraceId());
        logs.setTitle(event.getTitle());
        logs.setStatus(event.getStatus());
        logs.setErrorMsg(event.getErrorMsg());
        logs.setNickname(event.getNickname());
        logs.setRequestTime(event.getRequestTime());
        logs.setOperLocation(event.getOperLocation());
        return logs;
    }
}