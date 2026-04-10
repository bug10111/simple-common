package com.simple.common.logs.client.common.converter;

import com.simple.common.logs.client.common.event.LogDataEvent;
import com.simple.common.logs.proto.LogData;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * LogData 与 LogDataEvent 转换工具类
 * 提供 Protobuf 消息与业务对象之间的双向转换
 *
 * @author Admin
 */
public class LogDataConverter {

    private LogDataConverter() {
        // 私有构造函数，防止实例化
    }

    /**
     * 将 LogDataEvent 转换为 LogData Protobuf 消息
     *
     * @param event 日志事件对象
     * @return LogData Protobuf 消息
     */
    public static LogData toProto(LogDataEvent event) {
        if (event == null) {
            return null;
        }

        LogData.Builder builder = LogData.newBuilder();
        
        if (event.getTraceId() != null) {
            builder.setTraceId(event.getTraceId());
        }
        if (event.getOperIp() != null) {
            builder.setOperIp(event.getOperIp());
        }
        if (event.getMethod() != null) {
            builder.setMethod(event.getMethod());
        }
        if (event.getOperUrl() != null) {
            builder.setOperUrl(event.getOperUrl());
        }
        if (event.getOperName() != null) {
            builder.setOperName(event.getOperName());
        }
        if (event.getOperParam() != null) {
            builder.setOperParam(event.getOperParam());
        }
        if (event.getOperResult() != null) {
            builder.setOperResult(event.getOperResult());
        }
        if (event.getErrorMessage() != null) {
            builder.setErrorMessage(event.getErrorMessage());
        }
        if (event.getOperTime() != null) {
            builder.setOperTime(event.getOperTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        if (event.getUserId() != null) {
            builder.setUserId(String.valueOf(event.getUserId()));
        }
        if (event.getUserName() != null) {
            builder.setUserName(event.getUserName());
        }
        if (event.getDeptId() != null) {
            builder.setDeptId(String.valueOf(event.getDeptId()));
        }
        if (event.getDeptName() != null) {
            builder.setDeptName(event.getDeptName());
        }
        if (event.getRequestMethod() != null) {
            builder.setRequestMethod(event.getRequestMethod());
        }
        if (event.getOperType() != null) {
            builder.setOperType(event.getOperType());
        }
        builder.setStatus(event.getStatus() != null ? event.getStatus() : 0);
        builder.setCostTime(event.getCostTime() != null ? event.getCostTime() : 0L);

        return builder.build();
    }

    /**
     * 将 LogData Protobuf 消息转换为 LogDataEvent
     *
     * @param proto LogData Protobuf 消息
     * @return LogDataEvent 日志事件对象
     */
    public static LogDataEvent toEvent(LogData proto) {
        if (proto == null) {
            return null;
        }

        LogDataEvent event = new LogDataEvent();
        
        if (!proto.getTraceId().isEmpty()) {
            event.setTraceId(proto.getTraceId());
        }
        if (!proto.getOperIp().isEmpty()) {
            event.setOperIp(proto.getOperIp());
        }
        if (!proto.getMethod().isEmpty()) {
            event.setMethod(proto.getMethod());
        }
        if (!proto.getOperUrl().isEmpty()) {
            event.setOperUrl(proto.getOperUrl());
        }
        if (!proto.getOperName().isEmpty()) {
            event.setOperName(proto.getOperName());
        }
        if (!proto.getOperParam().isEmpty()) {
            event.setOperParam(proto.getOperParam());
        }
        if (!proto.getOperResult().isEmpty()) {
            event.setOperResult(proto.getOperResult());
        }
        if (!proto.getErrorMessage().isEmpty()) {
            event.setErrorMessage(proto.getErrorMessage());
        }
        if (proto.getOperTime() > 0) {
            event.setOperTime(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(proto.getOperTime()), 
                    ZoneId.systemDefault()));
        }
        if (!proto.getUserId().isEmpty()) {
            event.setUserId(Long.parseLong(proto.getUserId()));
        }
        if (!proto.getUserName().isEmpty()) {
            event.setUserName(proto.getUserName());
        }
        if (!proto.getDeptId().isEmpty()) {
            event.setDeptId(Long.parseLong(proto.getDeptId()));
        }
        if (!proto.getDeptName().isEmpty()) {
            event.setDeptName(proto.getDeptName());
        }
        if (!proto.getRequestMethod().isEmpty()) {
            event.setRequestMethod(proto.getRequestMethod());
        }
        if (!proto.getOperType().isEmpty()) {
            event.setOperType(proto.getOperType());
        }
        event.setStatus(proto.getStatus());
        event.setCostTime(proto.getCostTime());

        return event;
    }
}
