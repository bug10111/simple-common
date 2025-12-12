package com.simple.common.eventbus.common.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.simple.common.core.common.service.cycle.AbsCycleService;
import com.simple.common.eventbus.common.event.CycleEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 * Description: 基于事件的计划任务
 *
 * @author 兄台丶请冷静
 */
@Slf4j
public abstract class AbsEventCycleService<T> extends AbsCycleService<T> {

    @Autowired
    private EventBusService eventBusService;

    public AbsEventCycleService(Class<T> eventClass) {
        super(eventClass);
    }

    @Override
    public void run(T runBody, Integer sum, Integer timeInterval, Boolean isAccumulate, Map<String, Object> parameters) {
        CycleEvent event = new CycleEvent();
        event.setRunBody(runBody);
        event.setSum(sum);
        event.setNum(0);
        event.setTimeInterval(timeInterval);
        event.setIsAccumulate(isAccumulate);
        event.setReserve(parameters);
        execution(event);
    }

    @SuppressWarnings("unchecked")
    public void execution(CycleEvent event) {
        T runBody;
        if (event.getRunBody() instanceof JSONObject) {
            runBody = toBean(event.getRunBody());
        } else {
            runBody = (T) event.getRunBody();
        }

        try {

            //执行任务
            boolean b = false;
            //第一次不执行，因为我的目的是第一次会延迟
            if(event.getNum() != 0){
                b = handler(runBody, event.getReserve());
            }

            //没成功继续执行
            if (!b) {

                //小于的执行次数，继续执行
                if (event.getNum() < event.getSum()) {
                    //计数
                    event.setNum(event.getNum() + 1);
                    addCounter(runBody,event.getReserve());
                    event.setBeanName(StrUtil.lowerFirst(this.getClass().getSimpleName()));
                    eventBusService.push(event, event.getIsAccumulate() ? event.getTimeInterval() * event.getNum() : event.getTimeInterval(), TimeUnit.SECONDS);
                }

                //不继续执行
                else {
                    more(runBody, event.getReserve());
                }
            }

            //成功
            else {
                ok(runBody, event.getReserve());
            }
        } catch (Exception e) {
            log.error("调度异常：{}", String.valueOf(e));
            error(runBody, event.getReserve());
        }
    }
}
