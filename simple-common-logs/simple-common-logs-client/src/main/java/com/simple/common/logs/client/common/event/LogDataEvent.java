package com.simple.common.logs.client.common.event;

import cn.hutool.core.date.DateTime;
import com.simple.common.logs.proto.LogData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 日志数据事件
 * 用于传输日志数据
 * 这是整个日志模块唯一的数据模型定义，proto 模块和 server 模块都应使用此类
 *
 * @author qty
 */
@Data
public class LogDataEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * 方法名称/操作名称
     */
    private String title;

    /**
     * 请求方式
     */
    private String method;

    /**
     * 请求URL
     */
    private String operUrl;

    /**
     * 主机地址
     */
    private String operIp;

    /**
     * 操作地点
     */
    private String operLocation;

    /**
     * 操作人员id
     */
    private String userId;

    /**
     * 用户名
     */
    private String nickname;

    /**
     * 操作名称
     */
    private String operName;

    /**
     * 请求参数
     */
    private String operParam;

    /**
     * 操作状态
     */
    private int status;

    /**
     * 错误消息
     */
    private String errorMsg;

    /**
     * 异常信息
     */
    private String errorData;

    /**
     * 接口耗时（毫秒）
     */
    private Long requestTime;

    /**
     * 创建时间
     */
    private DateTime createTime;

    /**
     * 将 LogDataEvent 转换为 Protobuf LogData
     * 用于网络传输
     *
     * @return Protobuf LogData 对象
     */
    public LogData toProto() {
        LogData.Builder builder = LogData.newBuilder()
                                         .setTraceId(traceId != null ? traceId : "")
                                         .setTitle(title != null ? title : "")
                                         .setMethod(method != null ? method : "")
                                         .setOperUrl(operUrl != null ? operUrl : "")
                                         .setOperIp(operIp != null ? operIp : "")
                                         .setOperLocation(operLocation != null ? operLocation : "")
                                         .setUserId(userId != null ? userId : "")
                                         .setNickname(nickname != null ? nickname : "")
                                         .setOperName(operName != null ? operName : "")
                                         .setOperParam(operParam != null ? operParam : "")
                                         .setStatus(status)
                                         .setErrorMsg(errorMsg != null ? errorMsg : "")
                                         .setErrorData(errorData != null ? errorData : "")
                                         .setRequestTime(requestTime != null ? requestTime : 0L)
                                         .setCreateTime(createTime != null ? createTime.toTimestamp().getTime() : System.currentTimeMillis());
        return builder.build();
    }

    /**
     * 从 Protobuf LogData 创建 LogDataEvent
     * 用于服务端接收数据
     *
     * @param logData Protobuf LogData 对象
     * @return LogDataEvent 对象
     */
    public static LogDataEvent fromProto(LogData logData) {
        LogDataEvent event = new LogDataEvent();
        event.setTraceId(logData.getTraceId());
        event.setTitle(logData.getTitle());
        event.setMethod(logData.getMethod());
        event.setOperUrl(logData.getOperUrl());
        event.setOperIp(logData.getOperIp());
        event.setOperLocation(logData.getOperLocation());
        event.setUserId(logData.getUserId());
        event.setNickname(logData.getNickname());
        event.setOperName(logData.getOperName());
        event.setOperParam(logData.getOperParam());
        event.setStatus(logData.getStatus());
        event.setErrorMsg(logData.getErrorMsg());
        event.setErrorData(logData.getErrorData());
        event.setRequestTime(logData.getRequestTime());
        if (logData.getCreateTime() > 0) {
            event.setCreateTime(new DateTime(logData.getCreateTime()));
        }
        return event;
    }
}