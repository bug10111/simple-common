package com.simple.common.eventbus.service;

import com.simple.common.core.utils.AssertUtils;
import com.simple.common.eventbus.common.entity.EventData;
import com.simple.common.eventbus.util.MqNameUtil;
import com.simple.common.rabbitmq.common.service.RabbitMqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 异步事件执行器（基于RabbitMQ）
 * <p>投递语义：尽力而为——单个目标发送失败只记录日志并继续其余目标（目标间失败隔离），
 * 但存在失败时聚合输出汇总告警，避免「整体失败与成功不可区分、事件静默丢失」；
 * 延迟参数非法时回退为立即发送，不静默丢弃事件。</p>
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
        sendToTargets(eventData, 0, null);
    }

    /**
     * 发送延迟事件到所有目标系统
     * <p>延迟参数非法（time<=0 或 timeUnit 为空）时，底层延迟交换机会把消息按 0 延迟立即投递，
     * 调用方却以为已延迟——此处显式回退为立即发送并告警，保持与同步实现的语义一致</p>
     *
     * @param eventData 事件数据
     * @param time      延迟时间
     * @param timeUnit  时间单位
     */
    @Override
    protected void handler(EventData eventData, int time, TimeUnit timeUnit) {
        if (time <= 0 || timeUnit == null) {
            log.warn("延迟参数非法（time={} , timeUnit={}），事件 [{}] 回退为立即发送",
                    time, timeUnit, eventData == null ? null : eventData.getEventName());
            sendToTargets(eventData, 0, null);
            return;
        }
        sendToTargets(eventData, time, timeUnit);
    }

    /**
     * 遍历目标系统逐个投递（立即/延迟统一入口）
     * <p>空白目标跳过并告警（底层只做字符串拼接，空白目标会生成非法交换机名后静默丢消息）；
     * 单个目标失败不中断其余目标，结束后聚合汇总告警，保证总/部失败对运维可观测。</p>
     *
     * @param eventData 事件数据（不能为空，且目标列表不能为空）
     * @param time      延迟时间，0 表示立即发送
     * @param timeUnit  延迟时间单位，仅 time>0 时使用
     */
    private void sendToTargets(EventData eventData, int time, TimeUnit timeUnit) {
        AssertUtils.notNull(eventData, "事件数据不能为空");
        List<String> targets = eventData.getObjectivesApplication();
        if (targets == null || targets.isEmpty()) {
            // 空目标 = 事件无处可发：显式告警，避免静默丢弃
            log.warn("事件目标系统为空，事件未发送。事件名称: {}", eventData.getEventName());
            return;
        }

        List<String> failedTargets = new ArrayList<>(targets.size());
        for (String target : targets) {
            if (!StringUtils.hasText(target)) {
                log.warn("事件 [{}] 存在空白目标系统，跳过发送", eventData.getEventName());
                continue;
            }
            try {
                if (time > 0) {
                    rabbitMqService.sendMsg(eventData, MqNameUtil.delayExchangeName(target),
                            MqNameUtil.keyName(target), time, timeUnit);
                } else {
                    rabbitMqService.sendMsg(eventData, MqNameUtil.exchangeName(target), MqNameUtil.keyName(target));
                }
            } catch (Exception e) {
                failedTargets.add(target);
                log.error("发送事件到目标 [{}] 失败，事件名称: {}", target, eventData.getEventName(), e);
                // 继续发送下一个目标，不因单个目标失败而中断
            }
        }

        if (!failedTargets.isEmpty()) {
            // 聚合告警：让总/部分投递失败对发布方与运维可见，而不是与成功不可区分
            log.warn("事件 [{}] 投递失败汇总：{}/{} 个目标失败 {}", eventData.getEventName(),
                    failedTargets.size(), targets.size(), failedTargets);
        }
    }
}
