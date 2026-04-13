package com.simple.common.rabbitmq.common.service;

import com.simple.common.rabbitmq.common.config.DefaultMessage;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 * Description: RabbitMQ服务提供接口
 *
 * @author qty
 */
public interface RabbitMqService {

    /**
     * spring boot 直接下发 8G2核 秒级5万左右，有调优空间
     *
     * @param defaultMessage 消息体
     * @param exchangeName   交换机名称
     * @param routingKey     路由key
     */
    void sendMsg(DefaultMessage defaultMessage, String exchangeName, String routingKey);

    /**
     * 基于插件的延时队列
     *
     * @param defaultMessage 消息体
     * @param exchangeName   交换机名称
     * @param routingKey     路由key
     * @param time           延迟时间
     * @param timeUnit       时间单位
     */
    void sendMsg(DefaultMessage defaultMessage, String exchangeName, String routingKey, int time, TimeUnit timeUnit);

    /**
     * 将时间和单位转化为毫秒
     *
     * @param time     时间
     * @param timeUnit 时间单位
     */
    default Long toMilliseconds(int time, TimeUnit timeUnit) {
        if (timeUnit == TimeUnit.HOURS) {
            return TimeUnit.HOURS.toMillis(time);
        } else if (timeUnit == TimeUnit.MINUTES) {
            return TimeUnit.MINUTES.toMillis(time);
        } else if (timeUnit == TimeUnit.SECONDS) {
            return TimeUnit.SECONDS.toMillis(time);
        } else if (timeUnit == TimeUnit.MILLISECONDS) {
            return TimeUnit.MILLISECONDS.toMillis(time);
        } else if (timeUnit == TimeUnit.DAYS) {
            return TimeUnit.DAYS.toMillis(time);
        } else {
            return 0L;
        }
    }

}