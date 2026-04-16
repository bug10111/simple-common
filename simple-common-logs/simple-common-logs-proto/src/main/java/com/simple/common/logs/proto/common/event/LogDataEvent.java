package com.simple.common.logs.proto.common.event;

import cn.hutool.core.date.DateTime;
import com.simple.common.logs.proto.common.time.DateTimeCache;
import com.simple.common.logs.proto.LogBatch;
import com.simple.common.logs.proto.LogData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
     * 创建时间戳（毫秒）
     */
    private long createTimestamp;

    // ==================== 对象创建与回收 ====================

    /**
     * 获取一个新的 LogDataEvent 实例
     * <p>
     * <b>使用规范（重要）</b>：
     * 每次调用均返回全新实例，避免跨线程数据竞争问题。
     * 现代 JVM 对短生命周期对象回收效率极高，此方式对性能影响可忽略。
     * 发送完成后可调用 {@link #recycle()}（当前为空操作，保留以兼容原有调用）。
     * </p>
     *
     * @return 新的 LogDataEvent 实例
     */
    public static LogDataEvent acquire() {
        return new LogDataEvent();
    }

    /**
     * 将当前实例归还（当前版本无实际操作，保留以兼容原有调用）
     * <p>
     * 归还后不应再使用该实例的任何字段。
     * </p>
     */
    public void recycle() {
        // 无实际操作
    }

    /**
     * 获取创建时间（DateTime 对象）
     * <p>
     * 从秒级缓存获取 DateTime，避免重复创建对象。
     * 注意：返回的 DateTime 对象是缓存的，不应修改其内部状态。
     * </p>
     *
     * @return DateTime 对象（秒级缓存）
     */
    public DateTime getCreateTime() {
        return createTimestamp > 0 ? DateTimeCache.get(createTimestamp) : null;
    }

    /**
     * 设置创建时间（DateTime 对象）
     * <p>
     * 内部转换为时间戳存储。
     * </p>
     *
     * @param createTime DateTime 对象
     */
    public void setCreateTime(DateTime createTime) {
        this.createTimestamp = createTime != null ? createTime.getTime() : 0L;
    }

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
                                         .setCreateTime(createTimestamp > 0 ? createTimestamp : System.currentTimeMillis());
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
        event.setCreateTimestamp(logData.getCreateTime());
        return event;
    }

    /**
     * 重置对象字段（保留方法，供内部使用）
     * <p>
     * 将字段置为默认值。
     * </p>
     */
    public void reset() {
        this.traceId = null;
        this.title = null;
        this.method = null;
        this.operUrl = null;
        this.operIp = null;
        this.operLocation = null;
        this.userId = null;
        this.nickname = null;
        this.operName = null;
        this.operParam = null;
        this.status = 0;
        this.errorMsg = null;
        this.errorData = null;
        this.requestTime = null;
        this.createTimestamp = 0L;
    }

    /**
     * 将 LogDataEvent 列表转换为 Protobuf LogBatch 对象
     * <p>
     * 用于批量发送，将多条日志合并为单个网络消息。
     * </p>
     *
     * @param events 日志事件列表
     * @return LogBatch 对象
     */
    public static LogBatch toBatchProto(List<LogDataEvent> events) {
        LogBatch.Builder batchBuilder = LogBatch.newBuilder();
        for (LogDataEvent event : events) {
            batchBuilder.addLogs(event.toProto());
        }
        return batchBuilder.build();
    }

    /**
     * 从 Protobuf LogBatch 解析为 LogDataEvent 列表
     * <p>
     * 用于服务端批量处理，将单个批量消息解析为多个日志事件。
     * </p>
     *
     * @param batch LogBatch 对象
     * @return 日志事件列表
     */
    public static List<LogDataEvent> fromBatchProto(LogBatch batch) {
        List<LogDataEvent> events = new ArrayList<>(batch.getLogsCount());
        for (LogData logData : batch.getLogsList()) {
            events.add(fromProto(logData));
        }
        return events;
    }
}