package com.simple.common.core.common.service.cycle;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 循环执行某个方法，执行间隔时间根据累加规则计算
 *
 * @author qty
 */
public interface CycleService<T> {

    /**
     * 执行循环任务
     *
     * @param runBody      任务参数
     * @param sum          最大执行次数
     * @param timeInterval 基础时间间隔（单位：秒）
     * @param isAccumulate 是否累加延迟：true 表示延迟时间 = timeInterval × 当前次数，false 表示每次延迟固定为 timeInterval
     * @param parameters   扩展参数
     */
    void run(T runBody, Integer sum, Integer timeInterval, Boolean isAccumulate, Map<String, Object> parameters);

    /**
     * 均匀延迟时间执行（每次间隔固定为 timeInterval 秒）
     *
     * @param runBody      任务参数
     * @param sum          最大执行次数
     * @param timeInterval 基础时间间隔（单位：秒）
     */
    default void runUniform(T runBody, Integer sum, Integer timeInterval) {
        run(runBody, sum, timeInterval, false, new HashMap<>());
    }

    /**
     * 累加延迟时间执行（延迟时间 = timeInterval × 当前次数）
     *
     * @param runBody      任务参数
     * @param sum          最大执行次数
     * @param timeInterval 基础时间间隔（单位：秒）
     */
    default void runAccumulate(T runBody, Integer sum, Integer timeInterval) {
        run(runBody, sum, timeInterval, true, new HashMap<>());
    }

    /**
     * 累加延迟时间执行（延迟时间 = timeInterval × 当前次数）
     *
     * @param runBody      任务参数
     * @param sum          最大执行次数
     * @param timeInterval 基础时间间隔（单位：秒）
     * @param parameters   扩展参数
     */
    default void runAccumulate(T runBody, Integer sum, Integer timeInterval, Map<String, Object> parameters) {
        run(runBody, sum, timeInterval, true, parameters);
    }

    /**
     * 均匀延迟时间执行（每次间隔固定为 timeInterval 秒）
     *
     * @param runBody      任务参数
     * @param sum          最大执行次数
     * @param timeInterval 基础时间间隔（单位：秒）
     * @param parameters   扩展参数
     */
    default void runUniform(T runBody, Integer sum, Integer timeInterval, Map<String, Object> parameters) {
        run(runBody, sum, timeInterval, false, parameters);
    }
}