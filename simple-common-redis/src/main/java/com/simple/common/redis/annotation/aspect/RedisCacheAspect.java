package com.simple.common.redis.annotation.aspect;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.simple.common.core.function.ReturnValueFunction;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.redis.annotation.RedisCache;
import com.simple.common.redis.common.manager.RedisCacheAspectManager;
import com.simple.common.redis.common.properties.RedisCacheProperties;
import com.simple.common.redis.common.service.RedissonLockService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
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

        //获取注解相关参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Type returnType = signature.getMethod().getGenericReturnType();
        RedisCache redisCache = signature.getMethod().getAnnotation(RedisCache.class);

        String key = redisCacheAspectManager.getCacheKey(redisCache, joinPoint);
        String cacheKey = redisCacheProperties.isBag() ? redisCacheProperties.getDefaultBag() + ":" + key : key;
        var value = redisTemplate.opsForValue().get(cacheKey);

        //存在，则返回
        if (value != null) {
            return deserialize(value, redisCache, returnType);
        }

        //不存在
        else {
            ReturnValueFunction function = () -> {
                String saveValue = redisTemplate.opsForValue().get(cacheKey);

                //执行请求
                if (ObjUtil.isNull(saveValue)) {
                    if (log.isDebugEnabled()) {
                        log.debug("开始执行查询数据库！{}", joinPoint.getArgs());
                    }
                    //先对真实返回值判空，再序列化：JSON 序列化不会产出 null（null 会被渲染为字面量 "null"），
                    //先序列化后判空会让 no-return-value 配置永不生效（死代码）
                    Object result = joinPoint.proceed();
                    saveValue = ObjUtil.isNull(result) ? redisCacheProperties.getNoReturnValue() : JsonUtils.toJsonStr(result);
                    Integer cacheTime = redisCacheAspectManager.getCacheTime(redisCache.cacheTime(), redisCache.appendRandomDuration());
                    redisTemplate.opsForValue().set(cacheKey, saveValue, cacheTime, TimeUnit.SECONDS);
                }
                return saveValue;
            };

            Object lockValue = lockService.lockHaveValue(cacheKey, function);
            //锁实现返回 null（加锁失败等异常路径）时不能盲目 toString，否则 NPE 掩盖真实故障
            if (ObjUtil.isNull(lockValue)) {
                throw new IllegalStateException("获取缓存锁失败, key: " + cacheKey);
            }
            return deserialize(lockValue.toString(), redisCache, returnType);
        }

    }

    /**
     * 反序列化缓存值
     * <p>
     * 优先按方法真实返回类型（含泛型）反序列化：单一 Class 会丢失泛型信息，
     * {@code R<List<T>>} 等返回值的元素会退化为 JSONObject/Map；注解 aclass 仅在
     * 返回类型反序列化失败时作为后备（保持既有行为兼容）。
     *
     * @param value      缓存中的 JSON 文本
     * @param redisCache 注解配置
     * @param returnType 方法真实返回类型
     * @return 反序列化结果
     */
    private Object deserialize(String value, RedisCache redisCache, Type returnType) {
        try {
            return JSONUtil.toBean(value, returnType, false);
        } catch (Exception e) {
            log.debug("按返回类型反序列化失败，回退注解 aclass: {}", returnType, e);
            return JsonUtils.toJsonObj(value, redisCache.aclass());
        }
    }
}
