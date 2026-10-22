package com.simple.common.redis.manager;

import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONUtil;
import com.simple.common.core.utils.HttpServletUtils;
import com.simple.common.redis.annotation.RedisCache;
import com.simple.common.redis.common.constant.RedisCacheConstant;
import com.simple.common.redis.common.manager.RedisCacheAspectManager;
import com.simple.common.redis.common.properties.RedisCacheProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 默认缓存 Key 生成策略。
 *
 * @author qty
 */
@Slf4j
@Service
public class DefaultRedisCacheAspectManager implements RedisCacheAspectManager {

    @Autowired
    private RedisCacheProperties redisCacheProperties;

    @Override
    public String getCacheKey(RedisCache redisCache, ProceedingJoinPoint joinPoint) {
        StringBuilder builder = new StringBuilder();
        if (RedisCacheConstant.REQ_URL.equals(redisCache.head())) {
            builder.append(resolveRequestUri(joinPoint));
        } else {
            builder.append(redisCache.head());
        }

        // 方法签名入键：避免同一 URI+参数文本下不同方法的缓存条目互相污染
        builder.append("#").append(joinPoint.getSignature().toShortString());

        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            builder.append("&").append(redisCacheProperties.getNoParametersKey());
        } else {
            // 参数做 JSON 序列化哈希，而非 Arrays.toString：identity toString 对未重写对象
            // 产出地址串（同一逻辑调用每次键漂移，缓存恒失效），且 null 与字面 "null" 无法区分；
            // JSON 序列化对字段值、null 与字面量表达确定且可复现
            builder.append("&").append(MD5.create().digestHex(JSONUtil.toJsonStr(args)));
        }
        return builder.toString();
    }

    /**
     * 解析当前请求 URI；非 Web 线程（异步/定时/MQ/测试）无请求上下文时降级为切点签名，
     * 限流/缓存检查失败不应中断业务调用本身。
     */
    private String resolveRequestUri(ProceedingJoinPoint joinPoint) {
        try {
            HttpServletRequest request = HttpServletUtils.getRequest();
            return request.getRequestURI();
        } catch (Exception e) {
            return "non-web:" + joinPoint.getSignature().toShortString();
        }
    }

    @Override
    public int getCacheTime(int cacheTime, int appendRandomDuration) {
        // 不追加随机时长时直接返回基础缓存时间，避免随机数上限非正数导致异常
        if (appendRandomDuration <= 0) {
            return cacheTime;
        }
        // 追加随机时长，打散过期时间点防止缓存雪崩
        return cacheTime + cn.hutool.core.util.RandomUtil.randomInt(0, appendRandomDuration);
    }
}
