package com.simple.common.redis.manager;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.simple.common.core.common.properties.LockProperties;
import com.simple.common.core.common.service.jwt.CoreLoginUserService;
import com.simple.common.core.utils.HttpServletUtils;
import com.simple.common.core.utils.IPUtils;
import com.simple.common.redis.annotation.CurrentLimiting;
import com.simple.common.redis.common.enums.CurrentLimitingRulesEnum;
import com.simple.common.redis.common.manager.CurrentLimitingManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateLimiterConfig;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson 的限流管理器。
 *
 * @author qty
 */
@Slf4j
@Service
public class RedissonCurrentLimitingManager implements CurrentLimitingManager {

    @Autowired
    private RedissonClient redisson;

    @Autowired
    private LockProperties lockProperties;

    @Override
    public Boolean execute(CurrentLimiting currentLimiting) {
        // 注解参数前置校验：非法 sum/time 若等到 Redisson 首次请求才抛 IAE，问题会以难定位的形态暴露
        if (currentLimiting.time() <= 0 || currentLimiting.sum() <= 0) {
            throw new IllegalArgumentException("限流注解参数非法: time/sum 必须为正整数, time="
                    + currentLimiting.time() + ", sum=" + currentLimiting.sum());
        }
        // 等待时长收敛为非负：负值会被 Redisson 拒绝；0 表示立即失败（默认 fail-fast，不长时间占用请求线程）
        long waitSeconds = Math.max(0, currentLimiting.waitingTime());

        String key = getKey(currentLimiting);
        if (lockProperties.isBag()) {
            key = lockProperties.getDefaultBag() + ":" + key;
        }
        RRateLimiter rateLimiter = getRateLimiter(key, currentLimiting);
        return rateLimiter.tryAcquire(waitSeconds, TimeUnit.SECONDS);
    }

    /**
     * 获取限流规则 Key
     * <p>
     * USER_ID 维度依赖业务工程实现的 CoreLoginUserService bean;
     * 业务工程未实现该 bean 时（无登录体系的部署形态）,降级使用 anonymous 作为限流key,
     * 使所有未登录请求在 USER_ID 维度合并为一个限流桶。
     * </p>
     */
    protected String getKey(CurrentLimiting currentLimiting) {
        HttpServletRequest request = HttpServletUtils.getRequest();
        CurrentLimitingRulesEnum keyType = currentLimiting.key();

        String keyStr = "";
        if (keyType == CurrentLimitingRulesEnum.URL) {
            keyStr = request.getRequestURI();
        } else if (keyType == CurrentLimitingRulesEnum.USER_ID) {
            // 用户维度绑定具体路由：否则同一用户的全部 USER_ID 端点共享一个桶，
            // 先被初始化的端点阈值会约束其余所有端点
            keyStr = resolveUserIdKey() + ":" + request.getRequestURI();
        } else if (keyType == CurrentLimitingRulesEnum.IP) {
            keyStr = IPUtils.getIpAddr() + ":" + request.getRequestURI();
        }
        return keyStr;
    }

    /**
     * 解析 USER_ID 维度的限流key
     * <p>
     * 优先从 Spring 容器获取 CoreLoginUserService 并取当前登录用户id;
     * 容器中不存在该 bean 时降级返回 anonymous,不中断限流检查;
     * 登录用户服务返回空白 id（匿名/未认证流量）时同样归入 anonymous 桶，
     * 避免 null/空串成为独立键或全部请求共享同一个无主桶。
     * </p>
     *
     * @return 当前登录用户id;无登录体系或匿名请求时返回 anonymous
     */
    protected String resolveUserIdKey() {
        CoreLoginUserService loginUserService = getLoginUserService();

        //业务工程未实现登录用户服务时降级为匿名桶
        if (loginUserService == null) {
            log.warn("未获取到userId，请实现CoreLoginUserService的getUserId方法；USER_ID 维度限流降级进入匿名桶");
            return "anonymous";
        }
        String userId = loginUserService.getUserId();
        // 匿名/未认证流量统一进入匿名桶
        return StrUtil.isBlank(userId) ? "anonymous" : userId;
    }

    /**
     * 从 Spring 容器获取 CoreLoginUserService
     * <p>
     * 容器中不存在该 bean 时 SpringUtil.getBean 会抛出 NoSuchBeanDefinitionException，
     * 此处仅捕获该「无 bean」情形返回 null，由调用方执行降级逻辑；
     * 其余 BeansException（如 NoUniqueBeanDefinitionException、BeanCreationException）
     * 属于真实装配错误，向外抛出以暴露问题，不被降级逻辑掩盖。
     * </p>
     *
     * @return CoreLoginUserService 实例;容器中无该 bean 时返回 null
     */
    private CoreLoginUserService getLoginUserService() {
        try {
            return SpringUtil.getBean(CoreLoginUserService.class);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    /**
     * 获取或创建限流器，使用细粒度锁避免全局阻塞。
     * <p>
     * 已存在的限流器会先按最新注解配置对齐（见 {@link #alignRateConfig}），
     * 避免调参后因 Redisson 限流器状态无 TTL 永久留存而永不生效。
     */
    protected RRateLimiter getRateLimiter(String key, CurrentLimiting currentLimiting) {
        RRateLimiter rateLimiter = redisson.getRateLimiter(key);
        // 快速路径：已存在则对齐配置后直接返回
        if (rateLimiter.isExists()) {
            alignRateConfig(rateLimiter, currentLimiting, key);
            return rateLimiter;
        }
        // 使用分布式锁初始化，防止并发重复创建
        String initLockKey = key + ":init";
        var lock = redisson.getLock(initLockKey);
        try {
            lock.lock(5, TimeUnit.SECONDS);
            if (!rateLimiter.isExists()) {
                boolean success = rateLimiter.trySetRate(RateType.OVERALL, currentLimiting.sum(), currentLimiting.time(), RateIntervalUnit.SECONDS);
                if (!success) {
                    log.warn("限流器 [{}] 初始化失败，可能已被其他进程设置", key);
                }
            } else {
                alignRateConfig(rateLimiter, currentLimiting, key);
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return rateLimiter;
    }

    /**
     * 将已存在限流器的配置与最新注解配置对齐
     * <p>
     * Redisson 限流器的配置一经 trySetRate 落库即无 TTL 永久留存，注解调整 sum/time 后
     * 若不对齐，新阈值永不生效。检测到 rate 或时间窗不一致时以最新配置覆盖；
     * 多实例并发执行 setRate 写入相同目标值，结果幂等。
     *
     * @param rateLimiter     已存在的限流器
     * @param currentLimiting 最新限流注解配置
     * @param key             限流器键（用于日志）
     */
    protected void alignRateConfig(RRateLimiter rateLimiter, CurrentLimiting currentLimiting, String key) {
        RateLimiterConfig config = rateLimiter.getConfig();
        long expectedRate = currentLimiting.sum();
        long expectedIntervalMillis = TimeUnit.SECONDS.toMillis(currentLimiting.time());
        if (config.getRate() != expectedRate || config.getRateInterval() != expectedIntervalMillis) {
            rateLimiter.setRate(RateType.OVERALL, expectedRate, currentLimiting.time(), RateIntervalUnit.SECONDS);
            log.info("限流器 [{}] 配置已按最新注解对齐: rate={}/{}s", key, expectedRate, currentLimiting.time());
        }
    }
}
