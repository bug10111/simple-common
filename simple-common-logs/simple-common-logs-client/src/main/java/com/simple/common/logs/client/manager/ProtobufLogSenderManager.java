package com.simple.common.logs.client.manager;

import com.simple.common.logs.client.common.converter.LogDataConverter;
import com.simple.common.logs.client.common.event.LogDataEvent;
import com.simple.common.logs.client.common.manager.LogSenderManager;
import com.simple.common.logs.client.common.tcp.LogProtobufTcpClient;
import com.simple.common.logs.proto.LogData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Protobuf 日志发送管理器
 * 使用 Protobuf 协议发送日志数据
 *
 * @author Admin
 */
@Slf4j
@RequiredArgsConstructor
public class ProtobufLogSenderManager implements LogSenderManager {

    private final LogProtobufTcpClient tcpClient;

    @Override
    public boolean send(LogDataEvent event) {
        try {
            // 转换为 Protobuf 消息
            LogData logData = LogDataConverter.toProto(event);
            // 发送
            tcpClient.send(logData);
            return true;
        } catch (Exception e) {
            log.error("发送日志数据失败", e);
            return false;
        }
    }

    /**
     * 关闭客户端
     */
    public void shutdown() {
        tcpClient.shutdown();
    }

    /**
     * 检查客户端是否活跃
     */
    public boolean isActive() {
        return tcpClient.isActive();
    }
}