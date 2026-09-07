package com.simple.common.redis.manager;

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
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeansException;
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
        String rules = getKey(currentLimiting);
        if (lockProperties.isBag()) {
            rules = lockProperties.getDefaultBag() + ":" + rules;
        }
        RRateLimiter rateLimiter = getRateLimiter(rules, currentLimiting);
        return rateLimiter.tryAcquire(currentLimiting.waitingTime(), TimeUnit.SECONDS);
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
            keyStr = resolveUserIdKey();
        } else if (keyType == CurrentLimitingRulesEnum.IP) {
            keyStr = IPUtils.getIpAddr() + ":" + request.getRequestURI();
        }
        return keyStr;
    }

    /**
     * 解析 USER_ID 维度的限流key
     * <p>
     * 优先从 Spring 容器获取 CoreLoginUserService 并取当前登录用户id;
     * 容器中不存在该 bean 时（含 NoSuchBeanDefinitionException 等 BeansException）降级返回 anonymous,
     * 不中断限流检查。
     * </p>
     *
     * @return 当前登录用户id;无登录体系部署形态下返回 anonymous
     */
    protected String resolveUserIdKey() {
        CoreLoginUserService loginUserService = getLoginUserService();

        //业务工程未实现登录用户服务时降级为匿名桶
        if (loginUserService == null) {
            log.error("未获取到userId，请实现CoreLoginUserService的getUserId方法");
            return "anonymous";
        }
        return loginUserService.getUserId();
    }

    /**
     * 从 Spring 容器获取 CoreLoginUserService
     * <p>
     * 容器中不存在该 bean 时 SpringUtil.getBean 会抛出异常,此处捕获 BeansException
     * （含 NoSuchBeanDefinitionException）返回 null,由调用方执行降级逻辑。
     * </p>
     *
     * @return CoreLoginUserService 实例;容器中无该 bean 时返回 null
     */
    private CoreLoginUserService getLoginUserService() {
        try {
            return SpringUtil.getBean(CoreLoginUserService.class);
        } catch (BeansException e) {
            return null;
        }
    }

    /**
     * 获取或创建限流器，使用细粒度锁避免全局阻塞。
     */
    protected RRateLimiter getRateLimiter(String key, CurrentLimiting currentLimiting) {
        RRateLimiter rateLimiter = redisson.getRateLimiter(key);
        // 快速路径：已存在直接返回
        if (rateLimiter.isExists()) {
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
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return rateLimiter;
    }
}