package com.simple.common.logs.client.manager;

import com.simple.common.core.utils.SerializeUtils;
import com.simple.common.logs.client.common.event.LogDataEvent;
import com.simple.common.logs.client.common.manager.LogSenderManager;
import com.simple.common.logs.client.common.properties.LogTcpClientProperties;
import com.simple.common.logs.client.common.tcp.LogTcpClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * TCP日志发送器实现
 *
 * @author qty
 */
@Slf4j
@Component
public class TcpLogSenderManager implements LogSenderManager {

    @Autowired
    private  LogTcpClient tcpClient;

    @Override
    public boolean send(LogDataEvent logData) {
        byte[] data = SerializeUtils.serialize(logData);
        return tcpClient.send(data);
    }

}