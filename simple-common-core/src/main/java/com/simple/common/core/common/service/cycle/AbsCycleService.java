package com.simple.common.core.common.service.cycle;

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

    @Autowired
    private ThreadService threadService;

    public abstract Class<T> getEventClass();

    @Override
    public void run(T runBody, Integer sum, Integer timeInterval, Boolean isAccumulate, Map<String, Object> parameters) {
        // 入参规范化：缺省值兜底，避免null自动拆箱在深层递归里变成裸NPE
        int safeSum = (sum == null || sum <= 0) ? 1 : sum;
        int safeInterval = (timeInterval == null || timeInterval < 0) ? 0 : timeInterval;
        boolean safeAccumulate = Boolean.TRUE.equals(isAccumulate);
        execution(runBody, safeSum, 0, safeInterval, safeAccumulate, parameters);
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
    protected void execution(T runBody, int sum, int num, int timeInterval, boolean isAccumulate, Map<String, Object> parameters) {
        // 当前执行次数 = 已执行次数 + 1
        int currentNum = num + 1;

        if (currentNum <= sum) {
            // 仅对真正会执行handler的分支计数，终态more()分支不再多计一次
            addCounter(runBody, parameters, currentNum);

            if (log.isDebugEnabled()) {
                log.debug("开始第{}次调度任务，参数：[{}]", currentNum, JsonUtils.toJsonStr(runBody));
            }

            // 计算延迟时间：long运算防大间隔*大次数int溢出为负；负值会被调度器当0立即触发
            long delay = isAccumulate ? (long) timeInterval * currentNum : timeInterval;
            if (delay < 0) {
                delay = 0L;
            }

            threadService.schedule(() -> {
                try {
                    boolean success = Boolean.TRUE.equals(handler(runBody, parameters));

                    if (!success) {
                        // 递归时传递正确的当前执行次数 currentNum
                        execution(runBody, sum, currentNum, timeInterval, isAccumulate, parameters);
                    } else {
                        ok(runBody, parameters);
                    }
                } catch (Exception e) {
                    // 业务异常与"返回false"同语义：还有剩余次数就继续重试，次数耗尽才进入error，
                    // 避免瞬时异常把整个周期永久打死；Error(OOM等)向上传播由线程未捕获处理器接管
                    log.error("调度异常：第{}次执行失败", currentNum, e);
                    if (currentNum < sum) {
                        execution(runBody, sum, currentNum, timeInterval, isAccumulate, parameters);
                    } else {
                        error(runBody, parameters);
                    }
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
        // 惰性解析目标类型：不再在构造器缓存（构造期子类字段未初始化，getEventClass()依赖字段时会得到null）
        Class<T> eventClass = getEventClass();
        if (eventClass == null) {
            throw new IllegalStateException("getEventClass() 不能返回null: " + getClass().getName());
        }
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
     * @return 是否执行成功（null按失败处理）
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
