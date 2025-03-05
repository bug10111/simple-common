package com.simple.common.auth.client.service;

import com.simple.common.auth.client.common.properties.CsrfProperties;
import com.simple.common.core.utils.AssertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Service
public class DefaultCsrfService extends AbsCsrfService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CsrfProperties csrfProperties;

    @Override
    public void saveToken(String userId, String path, String token) {
        redisTemplate.opsForValue().set(csrfProperties.getKey(path, userId), token, csrfProperties.getCacheTime(), TimeUnit.SECONDS);
    }

    @Override
    public String getToken(String userId, String path) {
        String token = redisTemplate.opsForValue().get(csrfProperties.getKey(path, userId));
        AssertUtils.notEmpty(token, "已提交", "用户[{}]==>[{}]没有保存的CSRF Token", userId, path);
        return token;
    }

    @Override
    public void removeToken(String userId, String path) {
        redisTemplate.delete(csrfProperties.getKey(path, userId));
    }
}
