package com.simple.common.rabbitmq.common.manager;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
public interface RepeatRMQManager {

    /**
     * 标记已消费
     *
     * @param queue    queue
     * @param msgId    消息id
     * @param time     保存时间
     * @param timeUnit 时间单位
     */
    boolean register(String queue, String msgId, Integer time, TimeUnit timeUnit);

    /**
     * 更新时长
     * @param queue    queue
     * @param msgId    消息id
     * @param time     保存时间
     * @param timeUnit 时间单位
     */
    void update(String queue, String msgId, Integer time, TimeUnit timeUnit);

    /**
     * 删除已消费标记
     *
     * @param queue queue
     * @param msgId 消息id
     */
    void remove(String queue, String msgId);

}
