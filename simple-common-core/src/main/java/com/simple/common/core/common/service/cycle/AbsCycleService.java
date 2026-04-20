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

    private Class<T> eventClass;

    @Autowired
    private ThreadService threadService;

    public abstract Class<T> getEventClass();
    public AbsCycleService() {
        this.eventClass = getEventClass();
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
     * @param num          当前已执行次数（调用前已执行次数，调用时会自动 +1）
     * @param timeInterval 时间间隔
     * @param isAccumulate 是否时间累加
     * @param parameters   扩展参数
     */
    protected void execution(T runBody, Integer sum, Integer num, Integer timeInterval, Boolean isAccumulate, Map<String, Object> parameters) {
        // 【修复】当前执行次数 = 已执行次数 + 1
        int currentNum = num + 1;
        addCounter(runBody, parameters, currentNum);

        if (currentNum <= sum) {
            if (log.isDebugEnabled()) {
                log.debug("开始第{}次调度任务，参数：[{}]", currentNum, JsonUtils.toJsonStr(runBody));
            }

            // 计算延迟时间
            int delay = isAccumulate ? timeInterval * currentNum : timeInterval;

            threadService.schedule(() -> {
                try {
                    boolean success = handler(runBody, parameters);

                    if (!success) {
                        // 【修复】递归时传递正确的当前执行次数 currentNum
                        execution(runBody, sum, currentNum, timeInterval, isAccumulate, parameters);
                    } else {
                        ok(runBody, parameters);
                    }
                } catch (Throwable e) { // 【修复】捕获 Throwable 防止线程终止
                    log.error("调度异常：{}", e.getMessage(), e);
                    error(runBody, parameters);
                }
            }, delay, TimeUnit.SECONDS);
        } else {
            // 已达最大次数
            more(runBody, parameters);
        }
    }

    /**
     * 转换事件参数类型
     * 注意：若原始对象与目标类型不匹配，会通过JSON序列化再反序列化，
     * 可能导致Date、LocalDateTime等特殊类型转换异常，请确保参数类型兼容。
     */
    @SuppressWarnings("unchecked")
    protected T toBean(Object rawBody) {
        if (eventClass.isInstance(rawBody)) {
            return (T) rawBody;
        }
        // 使用JSON转换，注意类型兼容性
        return JsonUtils.toJsonObj(JsonUtils.toJsonStr(rawBody), eventClass);
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