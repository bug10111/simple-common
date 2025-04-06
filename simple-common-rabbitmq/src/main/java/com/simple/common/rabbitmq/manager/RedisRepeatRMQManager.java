package com.simple.common.rabbitmq.manager;

import com.simple.common.rabbitmq.common.manager.RepeatRMQManager;
import com.simple.common.rabbitmq.common.properties.RabbitMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 * Description: 重复消费
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Service
public class RedisRepeatRMQManager implements RepeatRMQManager {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitMqProperties rabbitMqProperties;

    @Override
    public boolean register(String queue, String msgId, Integer time, TimeUnit timeUnit) {
        Boolean b = redisTemplate.opsForValue().setIfAbsent(getRepeatedConsumptionKey(queue, msgId), "1", time, timeUnit);
        assert b != null : "redis再消息队列消费注册时缓存异常";
        return b;
    }

    @Override
    public void update(String queue, String msgId, Integer time, TimeUnit timeUnit) {
        redisTemplate.expire(getRepeatedConsumptionKey(queue, msgId),time, timeUnit);
    }

    @Override
    public void remove(String queue, String msgId) {
        redisTemplate.delete(getRepeatedConsumptionKey(queue, msgId));
    }

    /**
     * 获取消息队列执行状态的数据缓存key
     * 格式：是否消费 ：所属队列名 ：全局消息ID
     *
     * @param queue queue
     * @param msgId 自定义消息id
     */
    protected String getRepeatedConsumptionKey(String queue, String msgId) {
        return rabbitMqProperties.getWhetherToConsume() + ":" + queue + ":" + msgId;
    }

}
