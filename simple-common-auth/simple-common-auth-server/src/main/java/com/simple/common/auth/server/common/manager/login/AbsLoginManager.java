package com.simple.common.auth.server.common.manager.login;

import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.core.utils.AssertUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
public abstract class AbsLoginManager implements LoginManager {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private AuthProperties authProperties;

    /**
     * 校验登录失败次数
     *
     * @param key 登录标志，如账号 openid
     */
    protected void checkErrorNum(String key) {
        String num = stringRedisTemplate.opsForValue().get(authProperties.getKey(key));
        if (ObjUtil.isNotEmpty(num)) {
            assert num != null;
            if (Integer.parseInt(num) > authProperties.getLoginErrorNumber()) {
                log.error("登录失败！用户 [{}] 密码错误次数为 [{}] ", key, num);
                AssertUtils.error("已达到最大失败次数，请明日再试");
            }
        }
    }

    /**
     * 登录失败处理
     *
     * @param key 登录标志，如账号 openid
     */
    protected void loginError(String key) {
        Long increment = stringRedisTemplate.opsForValue().increment(authProperties.getKey(key), 1);
        if (increment != null && increment == 1) {
            stringRedisTemplate.expire(authProperties.getKey(key), authProperties.getLoginErrorTime(), TimeUnit.SECONDS);
        }
        AssertUtils.error("账号或者密码错误");
    }

}
