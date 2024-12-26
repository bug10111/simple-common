package com.simple.common.redis.manager;

import cn.hutool.core.util.RandomUtil;
import com.simple.common.core.utils.HttpServletUtils;
import com.simple.common.redis.annotation.RedisCache;
import com.simple.common.redis.common.constant.RedisCacheConstant;
import com.simple.common.redis.common.properties.RedisCacheProperties;
import com.simple.common.redis.common.manager.RedisCacheAspectManager;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Service
public class DefaultRedisCacheAspectManager implements RedisCacheAspectManager {

    @Autowired
    private RedisCacheProperties redisCacheProperties;

    @Override
    public String getCacheKey(RedisCache redisCache, ProceedingJoinPoint joinPoint) {
        StringBuilder item = new StringBuilder();
        if (RedisCacheConstant.REQ_URL.equals(redisCache.head())) {

            //获取请求URL作为缓存指向
            HttpServletRequest request = HttpServletUtils.getRequest();
            item.append(request.getRequestURI());
        } else {
            item.append(redisCache.head());
        }

        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            item.append("&");
            item.append(redisCacheProperties.getNoParametersKey());
        } else {
            for (Object str : args) {
                item.append("&");
                item.append(str);
            }
        }

        //        return SecureUtil.md5(item.toString());
        return item.toString();
    }

    @Override
    public Integer getCacheTime(Integer cacheTime, Integer appendRandomDuration) {
        return cacheTime + RandomUtil.randomInt(0, appendRandomDuration);
    }
}
