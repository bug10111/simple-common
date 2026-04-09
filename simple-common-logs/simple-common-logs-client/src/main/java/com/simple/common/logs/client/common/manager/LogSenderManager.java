package com.simple.common.logs.client.common.manager;

import com.simple.common.logs.client.common.event.LogDataEvent;

/**
 * 日志发送器接口
 *
 * @author qty
 */
public interface LogSenderManager {

    /**
     * 发送日志数据
     *
     * @param logData 日志数据
     * @return 是否发送成功
     */
    boolean send(LogDataEvent logData);

}