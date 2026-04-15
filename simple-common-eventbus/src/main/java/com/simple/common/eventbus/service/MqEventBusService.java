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
            // 避免修改原始对象，直接使用目标构建发送参数
            rabbitMqService.sendMsg(eventData, MqNameUtil.exchangeName(target), MqNameUtil.keyName(target));
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
            rabbitMqService.sendMsg(eventData, MqNameUtil.delayExchangeName(target),
                    MqNameUtil.keyName(target), time, timeUnit);
        }
    }
}