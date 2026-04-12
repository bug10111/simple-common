package com.simple.common.logs.client.common.manager;

import com.simple.common.logs.proto.common.event.LogDataEvent;

/**
 * 日志发送器接口
 *
 * @author qty
 */
public interface LogManager {

    /**
     * 发送日志数据
     *
     * @param logData 日志数据
     * @return 是否发送成功
     */
    boolean send(LogDataEvent logData);

    /**
     * 启动日志客户端
     */
    void start();

    /**
     * 停止日志客户端
     */
    void stop();

}