package com.simple.common.redis.common.manager;

import com.simple.common.redis.annotation.CurrentLimiting;

/**
 * 限流管理器接口。
 * <p>
 * 用于实现接口级别的访问限流，支持多种限流策略。
 * 默认实现 {@code RedissonCurrentLimitingManager} 基于
 * Redisson 的 RateLimiter 实现分布式限流。
 * </p>
 * <p>
 * 失败语义（fail-open，调用方必读）：Redis/Redisson 后端异常时实现方应放行请求（fail-open）——
 * 限流是辅助保护手段，不能反噬主链路；实现内部异常由 {@code CurrentLimitingAspect} 统一兜底放行。
 * </p>
 *
 * @author qty
 */
public interface CurrentLimitingManager {

    /**
     * 执行限流检查。
     * <p>
     * 根据限流注解配置的策略和 key，检查当前请求是否允许通过。
     * 返回值约定：TRUE=放行；非 TRUE（FALSE 或 null）=未获放行；后端异常不允许从此方法抛出，
     * 应在实现内部按 fail-open 约定处理（异常兜底见 {@code CurrentLimitingAspect}）。
     * </p>
     *
     * @param currentLimiting 限流注解配置，包含限流规则和阈值
     * @return TRUE 表示通过限流检查；非 TRUE 表示未获放行
     */
    Boolean execute(CurrentLimiting currentLimiting);

}
