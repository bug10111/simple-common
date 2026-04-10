package com.simple.common.logs.server.common.converter;

import com.simple.common.logs.proto.LogDataEvent;
import com.simple.common.logs.proto.LogData;
import com.simple.common.logs.server.common.entity.SysOperationLogs;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * LogData 与 SysOperationLogs 转换工具类
 * 提供 Protobuf 消息与实体对象之间的双向转换
 *
 * @author Admin
 */
public class LogDataConverter {

    private LogDataConverter() {
        // 私有构造函数，防止实例化
    }

    /**
     * 将 LogData Protobuf 消息转换为 SysOperationLogs 实体
     *
     * @param proto LogData Protobuf 消息
     * @return SysOperationLogs 实体对象
     */
    public static SysOperationLogs toEntity(LogData proto) {
        if (proto == null) {
            return null;
        }

        SysOperationLogs entity = new SysOperationLogs();
        
        if (!proto.getTraceId().isEmpty()) {
            entity.setTraceId(proto.getTraceId());
        }
        if (!proto.getOperIp().isEmpty()) {
            entity.setOperIp(proto.getOperIp());
        }
        if (!proto.getMethod().isEmpty()) {
            entity.setMethod(proto.getMethod());
        }
        if (!proto.getOperUrl().isEmpty()) {
            entity.setOperUrl(proto.getOperUrl());
        }
        if (!proto.getOperName().isEmpty()) {
            entity.setOperName(proto.getOperName());
        }
        if (!proto.getOperParam().isEmpty()) {
            entity.setOperParam(proto.getOperParam());
        }
        if (!proto.getOperResult().isEmpty()) {
            entity.setOperResult(proto.getOperResult());
        }
        if (!proto.getErrorMessage().isEmpty()) {
            entity.setErrorMessage(proto.getErrorMessage());
        }
        if (proto.getOperTime() > 0) {
            entity.setOperTime(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(proto.getOperTime()), 
                    ZoneId.systemDefault()));
        }
        if (!proto.getUserId().isEmpty()) {
            entity.setUserId(Long.parseLong(proto.getUserId()));
        }
        if (!proto.getUserName().isEmpty()) {
            entity.setUserName(proto.getUserName());
        }
        if (!proto.getDeptId().isEmpty()) {
            entity.setDeptId(Long.parseLong(proto.getDeptId()));
        }
        if (!proto.getDeptName().isEmpty()) {
            entity.setDeptName(proto.getDeptName());
        }
        if (!proto.getRequestMethod().isEmpty()) {
            entity.setRequestMethod(proto.getRequestMethod());
        }
        if (!proto.getOperType().isEmpty()) {
            entity.setOperType(proto.getOperType());
        }
        entity.setStatus(proto.getStatus());
        entity.setCostTime(proto.getCostTime());

        return entity;
    }

    /**
     * 将 SysOperationLogs 实体转换为 LogData Protobuf 消息
     *
     * @param entity SysOperationLogs 实体对象
     * @return LogData Protobuf 消息
     */
    public static LogData toProto(SysOperationLogs entity) {
        if (entity == null) {
            return null;
        }

        LogData.Builder builder = LogData.newBuilder();
        
        if (entity.getTraceId() != null) {
            builder.setTraceId(entity.getTraceId());
        }
        if (entity.getOperIp() != null) {
            builder.setOperIp(entity.getOperIp());
        }
        if (entity.getMethod() != null) {
            builder.setMethod(entity.getMethod());
        }
        if (entity.getOperUrl() != null) {
            builder.setOperUrl(entity.getOperUrl());
        }
        if (entity.getOperName() != null) {
            builder.setOperName(entity.getOperName());
        }
        if (entity.getOperParam() != null) {
            builder.setOperParam(entity.getOperParam());
        }
        if (entity.getOperResult() != null) {
            builder.setOperResult(entity.getOperResult());
        }
        if (entity.getErrorMessage() != null) {
            builder.setErrorMessage(entity.getErrorMessage());
        }
        if (entity.getOperTime() != null) {
            builder.setOperTime(entity.getOperTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        if (entity.getUserId() != null) {
            builder.setUserId(String.valueOf(entity.getUserId()));
        }
        if (entity.getUserName() != null) {
            builder.setUserName(entity.getUserName());
        }
        if (entity.getDeptId() != null) {
            builder.setDeptId(String.valueOf(entity.getDeptId()));
        }
        if (entity.getDeptName() != null) {
            builder.setDeptName(entity.getDeptName());
        }
        if (entity.getRequestMethod() != null) {
            builder.setRequestMethod(entity.getRequestMethod());
        }
        if (entity.getOperType() != null) {
            builder.setOperType(entity.getOperType());
        }
        builder.setStatus(entity.getStatus() != null ? entity.getStatus() : 0);
        builder.setCostTime(entity.getCostTime() != null ? entity.getCostTime() : 0L);

        return builder.build();
    }

    /**
     * 将 LogData Protobuf 消息转换为 LogDataEvent
     *
     * @param proto LogData Protobuf 消息
     * @return LogDataEvent 事件对象
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