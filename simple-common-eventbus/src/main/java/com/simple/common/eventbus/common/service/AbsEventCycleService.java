package com.simple.common.eventbus.common.service;

import cn.hutool.core.util.StrUtil;
import com.simple.common.core.common.service.cycle.AbsCycleService;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.eventbus.common.event.CycleEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于事件的计划任务抽象类
 *
 * @param <T> 任务参数类型
 * @author qty
 */
@Slf4j
public abstract class AbsEventCycleService<T> extends AbsCycleService<T> {

    @Autowired
    private EventBusService eventBusService;

    public AbsEventCycleService(Class<T> eventClass) {
        super(eventClass);
    }

    /**
     * 启动循环任务
     *
     * @param runBody      任务参数
     * @param sum          总执行次数
     * @param timeInterval 周期时间间隔
     * @param isAccumulate 是否累加延迟
     * @param parameters   扩展参数
     */
    @Override
    public void run(T runBody, Integer sum, Integer timeInterval, Boolean isAccumulate, Map<String, Object> parameters) {
        if (runBody == null) {
            log.error("任务参数不能为空");
            return;
        }

        if (sum == null || sum <= 0) {
            sum = 1; // 默认执行1次
        }

        if (timeInterval == null || timeInterval < 0) {
            timeInterval = 0; // 默认不延迟
        }

        if (isAccumulate == null) {
            isAccumulate = false; // 默认不累加
        }

        CycleEvent event = new CycleEvent();
        event.setRunBody(runBody);
        event.setSum(sum);
        event.setNum(0);
        event.setTimeInterval(timeInterval);
        event.setIsAccumulate(isAccumulate);
        event.setReserve(parameters);
        execution(event);
    }

    /**
     * 执行单次任务调度
     *
     * @param event 循环事件对象
     */
    public void execution(CycleEvent event) {
        if (event == null || event.getRunBody() == null) {
            log.error("循环事件对象或任务参数为空");
            return;
        }

        T runBody;
        try {
            runBody = toBean(event.getRunBody());
        } catch (Exception e) {
            log.error("转换任务参数失败", e);
            error(null, event.getReserve());
            return;
        }

        try {
            boolean success = handler(runBody, event.getReserve());

            if (!success) {
                handleFailure(event, runBody);
            } else {
                // 执行成功，回调成功处理
                ok(runBody, event.getReserve());
            }
        } catch (Exception e) {
            log.error("调度异常", e);
            handleException(event, runBody, e);
        }
    }

    /**
     * 处理执行失败的情况
     */
    private void handleFailure(CycleEvent event, T runBody) {
        int nextNum = event.getNum() + 1;
        if (nextNum <= event.getSum()) {
            // 计算延迟时间
            int delay = event.getIsAccumulate() ? event.getTimeInterval() * nextNum : event.getTimeInterval();
            event.setNum(nextNum);
            addCounter(runBody, event.getReserve(), nextNum);
            
            // 设置bean名称，用于后续查找
            event.setBeanName(StrUtil.lowerFirst(this.getClass().getSimpleName()));
            
            // 发送延迟事件
            eventBusService.push(event, delay, TimeUnit.SECONDS);
        } else {
            // 已达最大重试次数，执行最终失败处理
            more(runBody, event.getReserve());
        }
    }

    /**
     * 处理执行异常的情况
     */
    private void handleException(CycleEvent event, T runBody, Exception e) {
        if (event.getNum() < event.getSum()) {
            handleFailure(event, runBody);
        } else {
            error(runBody, event.getReserve());
        }
    }
}