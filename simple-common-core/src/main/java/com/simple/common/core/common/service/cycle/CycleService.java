package com.simple.common.core.common.service.cycle;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 循环执行某个方法，执行间隔时间=num * time
 *
 * @author 兄台丶请冷静
 */
public interface CycleService<T> {

    /**
     * 执行
     *
     * @param runBody      参数
     * @param sum          请求次数
     * @param isAccumulate 是否累加时间
     * @param timeInterval 时间间隔 = sum * timeInterval
     */
    void run(T runBody, Integer sum, Integer timeInterval, Boolean isAccumulate, Map<String, Object> parameters);


    /**
     * 均匀延迟时间执行
     *
     * @param runBody      参数
     * @param sum          请求次数
     * @param timeInterval 时间间隔 = sum * timeInterval
     */
    default void runUniform(T runBody, Integer sum, Integer timeInterval) {
        run(runBody, sum, timeInterval, false, new HashMap<>());
    }

    /**
     * 累加时间执行
     *
     * @param runBody      参数
     * @param sum          请求次数
     * @param timeInterval 时间间隔 = sum * timeInterval
     */
    default void runAccumulate(T runBody, Integer sum, Integer timeInterval) {
        run(runBody, sum, timeInterval, true, new HashMap<>());
    }

    /**
     * 累加时间执行
     *
     * @param runBody      参数
     * @param sum          请求次数
     * @param timeInterval 时间间隔 = sum * timeInterval
     * @param parameters 需要携带的参数
     */
    default void runAccumulate(T runBody, Integer sum, Integer timeInterval, Map<String, Object> parameters) {
        run(runBody, sum, timeInterval, true, parameters);
    }


    /**
     * 均匀延迟时间执行
     *
     * @param runBody      参数
     * @param sum          请求次数
     * @param timeInterval 时间间隔 = sum * timeInterval
     * @param parameters 需要携带的参数
     */
    default void runUniform(T runBody, Integer sum, Integer timeInterval, Map<String, Object> parameters) {
        run(runBody, sum, timeInterval, false, parameters);
    }
}
