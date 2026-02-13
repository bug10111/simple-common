package com.simple.common.core.common.service.cycle;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.simple.common.core.common.service.thread.ThreadService;
import com.simple.common.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
public abstract class AbsCycleService<T> implements CycleService<T> {

    private final Class<T> eventClass;

    @Autowired
    private ThreadService threadService;

    public AbsCycleService(Class<T> eventClass) {
        this.eventClass = eventClass;
    }

    @Override
    public void run(T runBody, Integer sum, Integer timeInterval, Boolean isAccumulate, Map<String, Object> parameters) {
        execution(runBody, sum, 0, timeInterval, isAccumulate, parameters);
    }

    /**
     * 具体的执行逻辑
     *
     * @param runBody      参数对象
     * @param sum          总共执行次数
     * @param num          当前次数
     * @param timeInterval 时间间隔
     * @param isAccumulate 是否时间累加
     */
    protected void execution(T runBody, Integer sum, Integer num, Integer timeInterval, Boolean isAccumulate, Map<String, Object> parameters) {
        num++;
        addCounter(runBody, parameters, num);

        //重要，复制到临时变量
        Integer finalNum = num;

        if (num <= sum) {
            if (log.isDebugEnabled()) {
                log.debug("开始第{}次调度任务，参数：[{}]", num, JsonUtils.toJsonStr(runBody));
            }
            threadService.schedule(() -> {
                try {

                    boolean b = handler(runBody, parameters);

                    //没成功继续执行
                    if (!b) {
                        execution(runBody, sum, finalNum, timeInterval, isAccumulate, parameters);
                    }

                    //成功
                    else {
                        ok(runBody, parameters);
                    }
                } catch (Exception e) {
                    log.error("调度异常：{}", String.valueOf(e));
                    error(runBody, parameters);
                }
            }, isAccumulate ? timeInterval * finalNum : timeInterval, TimeUnit.SECONDS);
        } else {
            more(runBody, parameters);
        }
    }

    /**
     * 格式化业务参数
     *
     * @param runBody 业务茶树
     */
    public T toBean(Object runBody) {
        return JSONUtil.toBean((JSONObject) runBody, eventClass);
    }

    /**
     * 需要执行的业务
     *
     * @param runBody 参数
     * @return 是否执行成功
     */
    protected abstract Boolean handler(T runBody, Map<String, Object> parameters);

    /**
     * 成功
     *
     * @param runBody 参数
     */
    protected abstract void ok(T runBody, Map<String, Object> parameters);

    /**
     * 超过最大次数
     *
     * @param runBody 参数
     */
    protected abstract void more(T runBody, Map<String, Object> parameters);

    /**
     * 执行异常
     *
     * @param runBody 参数
     */
    protected abstract void error(T runBody, Map<String, Object> parameters);

    /**
     * 增加计数器
     *
     * @param runBody    参数
     * @param parameters 头
     * @param count      当前重试次数
     */
    protected void addCounter(T runBody, Map<String, Object> parameters, int count) {

    }

}
