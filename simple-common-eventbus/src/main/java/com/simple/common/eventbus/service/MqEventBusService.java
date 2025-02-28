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
 * Created with IntelliJ IDEA
 * Description: 异步事件执行器
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "simple.event", name = "type", havingValue = "mq", matchIfMissing = true)
public class MqEventBusService extends AbsEventBusService {

    @Autowired
    private RabbitMqService rabbitMqService;

    @Override
    protected void handler(EventData eventData) {
        eventData.getObjectivesApplication().forEach(s -> {
            eventData.setObjectives(s);
            rabbitMqService.sendMsg(eventData, MqNameUtil.exchangeName(s), MqNameUtil.keyName(s));
        });
    }

    @Override
    protected void handler(EventData eventData, int time, TimeUnit timeUnit) {
        eventData.getObjectivesApplication().forEach(s -> {
            eventData.setObjectives(s);
            rabbitMqService.sendMsg(eventData, MqNameUtil.delayExchangeName(s), MqNameUtil.keyName(s), time, timeUnit);
        });
    }
}
