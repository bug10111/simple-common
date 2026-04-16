package com.simple.common.eventbus.service;

import com.simple.common.eventbus.common.entity.EventData;
import com.simple.common.eventbus.util.MqNameUtil;
import com.simple.common.rabbitmq.common.service.RabbitMqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 异步事件执行器（基于RabbitMQ）
 *
 * @author qty
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "simple.event", name = "type", havingValue = "mq", matchIfMissing = true)
public class MqEventBusService extends AbsEventBusService {

    @Autowired
    private RabbitMqService rabbitMqService;

    /**
     * 发送普通事件到所有目标系统
     *
     * @param eventData 事件数据
     */
    @Override
    protected void handler(EventData eventData) {
        for (String target : eventData.getObjectivesApplication()) {
            try {
                rabbitMqService.sendMsg(eventData, MqNameUtil.exchangeName(target), MqNameUtil.keyName(target));
            } catch (Exception e) {
                log.error("发送事件到目标 [{}] 失败，事件名称: {}", target, eventData.getEventName(), e);
                // 继续发送下一个目标，不因单个目标失败而中断
            }
        }
    }

    /**
     * 发送延迟事件到所有目标系统
     *
     * @param eventData 事件数据
     * @param time      延迟时间
     * @param timeUnit  时间单位
     */
    @Override
    protected void handler(EventData eventData, int time, TimeUnit timeUnit) {
        for (String target : eventData.getObjectivesApplication()) {
            try {
                rabbitMqService.sendMsg(eventData, MqNameUtil.delayExchangeName(target),
                        MqNameUtil.keyName(target), time, timeUnit);
            } catch (Exception e) {
                log.error("发送延迟事件到目标 [{}] 失败，事件名称: {}", target, eventData.getEventName(), e);
                // 继续发送下一个目标，不因单个目标失败而中断
            }
        }
    }
}