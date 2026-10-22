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
        // 登录键为 null 表示该处理器对此请求不参与计数（如 IP 未知时跳过 IP 维度计数）
        if (key == null) {
            return true;
        }
        String num = cacheManager.get(key);
        // 缓存值防御性解析：脏数据/异构写入导致非数字时按 0 处理并告警，不让解析异常伪装成登录失败
        int errorNum = 0;
        if (num != null && !num.isEmpty()) {
            try {
                errorNum = Integer.parseInt(num);
            } catch (NumberFormatException e) {
                log.warn("登录失败次数缓存值非法，按 0 处理，key: {}, value: {}", key, num);
            }
        }
        if (errorNum >= authProperties.getLoginErrorNumber()) {
            log.warn("登录失败次数超限，key: {}, 次数: {}", key, errorNum);
            return false;
        }
        return true;
    }

    /**
     * 记录登录失败。
     * <p>
     * 仅在计数器首次创建（increment == 1）时设置过期时间：每次失败都顺延 TTL 会让持续
     * 暴力尝试的客户端把锁定窗口无限拉长；INCR 与 EXPIRE 为两条独立命令，极端情况下
     * （两步之间进程中断）计数器可能无 TTL 存活，由 clearError（登录成功）或人工介入兜底。
     * </p>
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     * @param ip            请求 IP
     */
    @Override
    public void recordError(ClientDetails clientDetails, Object adapter, String ip) {
        String key = getKeyPrefix() + getLoginKey(clientDetails, adapter, ip);
        if (key == null) {
            return;
        }
        Long increment = cacheManager.increment(key, 1L);
        // 仅首次计数设置过期，避免每次失败顺延锁定窗口
        if (increment != null && increment == 1) {
            cacheManager.expire(key, authProperties.getLoginErrorTime());
        } else if (increment != null && cacheManager.getExpire(key) < 0) {
            // 自愈：INCR 与 EXPIRE 为两条独立命令，历史脏数据/极端中断会留下无 TTL 的计数器，
            // 按原始契约补设过期，杜绝计数器永久存活把账号/IP 永久锁死
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
        if (key == null) {
            return;
        }
        cacheManager.delete(key);
        log.debug("登录失败记录已清除，key: {}", key);
    }
}
