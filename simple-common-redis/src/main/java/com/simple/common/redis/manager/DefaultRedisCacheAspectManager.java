package com.simple.common.redis.manager;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.MD5;
import com.simple.common.core.utils.HttpServletUtils;
import com.simple.common.redis.annotation.RedisCache;
import com.simple.common.redis.common.constant.RedisCacheConstant;
import com.simple.common.redis.common.manager.RedisCacheAspectManager;
import com.simple.common.redis.common.properties.RedisCacheProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 默认缓存 Key 生成策略。
 *
 * @author qty
 */
@Service
public class DefaultRedisCacheAspectManager implements RedisCacheAspectManager {

    @Autowired
    private RedisCacheProperties redisCacheProperties;

    @Override
    public String getCacheKey(RedisCache redisCache, ProceedingJoinPoint joinPoint) {
        StringBuilder builder = new StringBuilder();
        if (RedisCacheConstant.REQ_URL.equals(redisCache.head())) {
            HttpServletRequest request = HttpServletUtils.getRequest();
            builder.append(request.getRequestURI());
        } else {
            builder.append(redisCache.head());
        }

        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            builder.append("&").append(redisCacheProperties.getNoParametersKey());
        } else {
            // 使用参数哈希值，避免Key过长且保证一致性
            builder.append("&").append(MD5.create().digestHex(Arrays.toString(args)));
        }
        return builder.toString();
    }

    @Override
    public Integer getCacheTime(Integer cacheTime, Integer appendRandomDuration) {
        return cacheTime + RandomUtil.randomInt(0, appendRandomDuration);
    }
}