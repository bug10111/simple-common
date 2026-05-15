package com.simple.common.auth.server.common.process;

import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.core.common.enums.process.DefaultKindProcess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 登录异常处理抽象基类。
 *
 * @author qty
 */
@Slf4j
public abstract class AbsLoginErrorProcess implements LoginErrorProcess {

    @Autowired
    @Qualifier("authCacheManager")
    protected CacheManager cacheManager;

    @Autowired
    protected AuthProperties authProperties;

    /**
     * 获取登录标识 key（如账号、IP 等）。
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     * @param ip            请求 IP
     * @return 标识 key
     */
    protected abstract String getLoginKey(ClientDetails clientDetails, Object adapter, String ip);

    /**
     * 获取 Redis 存储 key 的前缀。
     *
     * @return 前缀
     */
    protected  String getKeyPrefix(){
        DefaultKindProcess process = getProcess();
        return authProperties.getKey(process.getCode());
    }

    /**
     * 检查登录失败次数是否超限。
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     * @param ip            请求 IP
     * @return true 未超限，false 已超限
     */
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

    /**
     * 记录登录失败。
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     * @param ip            请求 IP
     */
    @Override
    public void recordError(ClientDetails clientDetails, Object adapter, String ip) {
        String key = getKeyPrefix() + getLoginKey(clientDetails, adapter, ip);
        Long increment = cacheManager.increment(key, 1L);
        if (increment != null && increment >= 1) {
            cacheManager.expire(key, authProperties.getLoginErrorTime());
        }
        log.debug("登录失败记录已更新，key: {}, 次数: {}", key, increment);
    }

    /**
     * 清除登录失败记录（登录成功后调用）。
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     * @param ip            请求 IP
     */
    @Override
    public void clearError(ClientDetails clientDetails, Object adapter, String ip) {
        String key = getKeyPrefix() + getLoginKey(clientDetails, adapter, ip);
        cacheManager.delete(key);
        log.debug("登录失败记录已清除，key: {}", key);
    }
}