package com.simple.common.redis.manager;

import cn.hutool.extra.spring.SpringUtil;
import com.simple.common.core.common.service.jwt.CoreLoginUserService;
import com.simple.common.core.utils.HttpServletUtils;
import com.simple.common.core.utils.IPUtils;
import com.simple.common.redis.annotation.CurrentLimiting;
import com.simple.common.redis.common.enums.CurrentLimitingRulesEnum;
import com.simple.common.redis.common.manager.CurrentLimitingManager;
import com.simple.common.core.common.properties.LockProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Service
public class RedissonCurrentLimitingManager implements CurrentLimitingManager<CurrentLimiting> {

    @Autowired
    private Redisson redisson;

    @Autowired
    private LockProperties lockProperties;

    @Override
    public Boolean execute(CurrentLimiting currentLimiting) {
        String rules = getKey(currentLimiting);
        if (lockProperties.isBag()) {
            rules = lockProperties.getDefaultBag() + ":" + rules;
        }
        RRateLimiter rRateLimiter = getRRateLimiter(rules, currentLimiting);
        return rRateLimiter.tryAcquire(currentLimiting.waitingTime(), TimeUnit.SECONDS);
    }

    /**
     * 获取限流规则
     *
     * @param currentLimiting 限流注解参数
     */
    protected String getKey(CurrentLimiting currentLimiting) {
        HttpServletRequest request = HttpServletUtils.getRequest();
        CurrentLimitingRulesEnum key = currentLimiting.key();

        String keyStr = "";
        if (key.equals(CurrentLimitingRulesEnum.URL)) {
            keyStr = request.getRequestURI();
        } else if (key.equals(CurrentLimitingRulesEnum.USER_ID)) {
            CoreLoginUserService bean = SpringUtil.getBean(CoreLoginUserService.class);
            if (bean != null) {
                keyStr = bean.getUserId();
            } else {
                log.error("未获取到userId,请实现CoreLoginUserService的getUserId方法");
            }
        } else if (key.equals(CurrentLimitingRulesEnum.IP)) {
            keyStr = IPUtils.getIpAddr() + request.getRequestURI();
        }
        return keyStr;
    }

    /**
     * 获取限流器
     *
     * @param key             限流器标志
     * @param currentLimiting 直接参数对象
     */
    protected RRateLimiter getRRateLimiter(String key, CurrentLimiting currentLimiting) {
        RRateLimiter rateLimiter = redisson.getRateLimiter(key);
        if (!rateLimiter.isExists()) {
            synchronized (RedissonCurrentLimitingManager.class){
                 rateLimiter = redisson.getRateLimiter(key);
                if (!rateLimiter.isExists()) {
                    rateLimiter.trySetRate(RateType.PER_CLIENT, currentLimiting.sum(), currentLimiting.time(), RateIntervalUnit.SECONDS);
                }
            }
        }
        return rateLimiter;
    }
}
