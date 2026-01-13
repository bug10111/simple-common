package com.simple.common.redis.annotation.aspect;

import cn.hutool.core.util.ObjUtil;
import com.simple.common.core.function.ReturnValueFunction;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.redis.annotation.RedisCache;
import com.simple.common.redis.common.service.RedissonLockService;
import com.simple.common.redis.common.manager.RedisCacheAspectManager;
import com.simple.common.redis.common.properties.RedisCacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 *
 * @author qty
 */
@Order(2)
@Aspect
@Slf4j
@Component
public class RedisCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisCacheAspectManager redisCacheAspectManager;

    @Autowired
    private RedissonLockService lockService;

    @Autowired
    private RedisCacheProperties redisCacheProperties;

    @Around("@annotation(com.simple.common.redis.annotation.RedisCache)")
    public Object around(ProceedingJoinPoint joinPoint) {

        //获取注解先关参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RedisCache redisCache = method.getAnnotation(RedisCache.class);

        String key = redisCacheAspectManager.getCacheKey(redisCache, joinPoint);
        String cacheKey = redisCacheProperties.isBag() ? redisCacheProperties.getDefaultBag() + ":" + key : key;
        var value = redisTemplate.opsForValue().get(cacheKey);

        //存在，则返回
        if (value != null) {
            return JsonUtils.toJsonObj(value, redisCache.aclass());
        }

        //不存在
        else {
            ReturnValueFunction function = () -> {
                String saveValue = redisTemplate.opsForValue().get(cacheKey);

                //执行请求
                if (ObjUtil.isNull(saveValue)) {
                    if(log.isDebugEnabled()){
                        log.debug("开始执行查询数据库！{}", joinPoint.getArgs());
                    }
                    saveValue = JsonUtils.toJsonStr(joinPoint.proceed(joinPoint.getArgs()));
                    Integer cacheTime = redisCacheAspectManager.getCacheTime(redisCache.cacheTime(), redisCache.appendRandomDuration());
                    saveValue = ObjUtil.isNull(saveValue) ? redisCacheProperties.getNoReturnValue() : saveValue;
                    redisTemplate.opsForValue().set(cacheKey, saveValue, cacheTime, TimeUnit.SECONDS);
                }
                return saveValue;
            };

            return JsonUtils.toJsonObj((lockService.lockHaveValue(key, function).toString()), redisCache.aclass());
        }

    }
}
