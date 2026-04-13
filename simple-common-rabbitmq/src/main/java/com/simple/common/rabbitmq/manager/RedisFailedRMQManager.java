package com.simple.common.rabbitmq.manager;

import cn.hutool.core.util.StrUtil;
import com.simple.common.rabbitmq.common.manager.FailedRMQManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 基于redis的消费失败处理
 *
 * @author qty
 */
@Component
public class RedisFailedRMQManager implements FailedRMQManager {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public Long increment(String failedKey) {
        return redisTemplate.opsForValue().increment(failedKey);
    }

    @Override
    public void delete(String failedKey) {
        redisTemplate.delete(failedKey);
    }

    @Override
    public String getFailedMqRedisKey(String calculation, String queue, String msgId) {
        return calculation + ":" + queue + ":" + msgId;
    }
}