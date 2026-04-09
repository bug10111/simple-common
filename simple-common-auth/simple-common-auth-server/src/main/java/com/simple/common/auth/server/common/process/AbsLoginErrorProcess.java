package com.simple.common.auth.server.common.process;

import com.simple.common.auth.server.common.entity.ClientDetails;

import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.server.common.enums.process.LoginErrorKindProcess;
import com.simple.common.auth.server.common.manager.cache.CacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created with IntelliJ IDEA
 * Description: 登录异常处理抽象基类
 *
 * @author qty
 */
@Slf4j
public abstract class AbsLoginErrorProcess implements LoginErrorProcess {

    @Autowired
    protected CacheManager cacheManager;

    @Autowired
    protected AuthProperties authProperties;

    /**
     * 获取登录标识key（如账号、IP等）
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     * @param ip            请求IP
     * @return 标识key
     */
    protected abstract String getLoginKey(ClientDetails clientDetails, Object adapter, String ip);

    /**
     * 获取Redis存储key的前缀
     *
     * @return 前缀
     */
    protected abstract String getKeyPrefix();

    @Override
    public boolean checkErrorNum(ClientDetails clientDetails, Object adapter, String ip) {
        String key = getKeyPrefix() + getLoginKey(clientDetails, adapter, ip);
        String num = cacheManager.get(key);
        if (num != null && Integer.parseInt(num) >= authProperties.getLoginErrorNumber()) {
            log.warn("登录失败次数超限，key: {}, 次数: {}", key, num);
            return false;
        }
        return true;
    }

    @Override
    public void recordError(ClientDetails clientDetails, Object adapter, String ip) {
        String key = getKeyPrefix() + getLoginKey(clientDetails, adapter, ip);
        Long increment = cacheManager.increment(key, 1);
        if (increment != null && increment == 1) {
            cacheManager.expire(key, authProperties.getLoginErrorTime());
        }
        log.debug("登录失败记录已更新，key: {}, 次数: {}", key, increment);
    }

    @Override
    public void clearError(ClientDetails clientDetails, Object adapter, String ip) {
        String key = getKeyPrefix() + getLoginKey(clientDetails, adapter, ip);
        cacheManager.delete(key);
        log.debug("登录失败记录已清除，key: {}", key);
    }
}