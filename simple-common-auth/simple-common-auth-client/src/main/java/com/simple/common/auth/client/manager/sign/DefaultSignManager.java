package com.simple.common.auth.client.manager.sign;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.client.common.manager.sign.SignManager;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.properties.SignProperties;
import com.simple.common.auth.client.util.SignSecretUtils;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.SignUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 默认签名管理器实现。
 * <p>
 * nonce 防重放走 CacheManager（signCacheManager，受 simple.sign.cache-type 控制）：
 * REDIS 形态下集群共享 nonce 记录，杜绝多实例间重放；LOCAL 形态仅本实例防重放（部署多实例时必须用 REDIS）。
 * nonce 记录 TTL 取 max(cacheTime, 时间窗口+60s)，保证 nonce 记录存活期严格覆盖时间戳窗口。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultSignManager implements SignManager {

    /**
     * nonce 防重放记录 TTL 的时间戳窗口冗余（秒）：确保 nonce 存活期 > 时间戳窗口
     */
    private static final long NONCE_TTL_SKEW_SECONDS = 60L;

    @Autowired
    private SignProperties signProperties;

    /**
     * nonce 防重放缓存（与业务缓存隔离；由 simple.sign.cache-type 决定 Redis/Local 形态）
     */
    private CacheManager nonceCache;

    /**
     * 依赖注入完成后初始化 nonce 缓存。
     */
    @PostConstruct
    public void init() {
        this.nonceCache = nonceCacheManager;
        long ttl = effectiveNonceTtlSeconds();
        log.info("签名管理器初始化完成，nonce 缓存形态: {}，nonce TTL: {}s，时间戳窗口: {}ms",
                signProperties.getCacheType(), ttl, signProperties.getDefaultTimeWindowMs());
        if (signProperties.getCacheTime() * 1000L < signProperties.getDefaultTimeWindowMs()) {
            log.warn("simple.sign.cache-time({}s) 小于时间戳窗口({}ms)：已按 窗口+60s 兜底延长 nonce TTL，防重放窗口不会被提前收缩",
                    signProperties.getCacheTime(), signProperties.getDefaultTimeWindowMs());
        }
    }

    @Autowired
    public void setNonceCacheManager(@Qualifier("signCacheManager") CacheManager nonceCacheManager) {
        this.nonceCacheManager = nonceCacheManager;
    }

    private CacheManager nonceCacheManager;

    /**
     * nonce 记录有效 TTL（秒）：必须严格覆盖时间戳窗口，否则 nonce 过期后窗口内的请求仍可重放。
     */
    private long effectiveNonceTtlSeconds() {
        long byWindow = signProperties.getDefaultTimeWindowMs() / 1000L + NONCE_TTL_SKEW_SECONDS;
        return Math.max(signProperties.getCacheTime(), byWindow);
    }

    /**
     * 校验时间戳是否在允许的时间窗口内。
     *
     * @param timestamp 请求头中的时间戳字符串（毫秒）
     */
    @Override
    public void checkTimestamp(String timestamp) {
        AssertUtils.notEmpty(timestamp, "时间戳不能为空");
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("时间戳格式错误");
        }
        // 拒绝非正数时间戳：now - Long.MIN_VALUE 会溢出使 Math.abs 为负，绕过窗口校验
        if (ts <= 0) {
            throw new IllegalArgumentException("时间戳格式错误");
        }
        long now = System.currentTimeMillis();
        long diff = Math.abs(now - ts);
        AssertUtils.isTrue(diff <= signProperties.getDefaultTimeWindowMs(), "请求已过期，时间差: " + diff + "ms");
        log.debug("时间戳校验通过，差值: {}ms", diff);
    }

    /**
     * 防重放校验：检查 nonce 是否已被使用。
     * <p>
     * 通过 CacheManager#setIfAbsent 原子性地「检查+记录」：并发携带相同 nonce 的请求
     * 有且仅有一个通过（check-then-act 竞态已消除）；REDIS 形态下集群共享记录。
     * </p>
     *
     * @param nonce 随机数字符串
     */
    @Override
    public void checkNonce(String nonce) {
        AssertUtils.notEmpty(nonce, "随机数不能为空");
        String nonceKey = signProperties.getSignKey() + "nonce:" + nonce;
        boolean firstUse = nonceCache.setIfAbsent(nonceKey, "1", effectiveNonceTtlSeconds());
        AssertUtils.isTrue(firstUse, "非法请求");
    }

    /**
     * 生成 HMAC-SHA256 签名。
     *
     * @param message 待签名的内容
     * @return 十六进制签名字符串
     */
    @Override
    public String signWeb(String message) {
        AssertUtils.notEmpty(message, "签名内容不能为空");
        String key = SignSecretUtils.getSecret();
        return SignUtils.signWeb(message, key);
    }

    /**
     * 验证 HMAC-SHA256 签名。
     *
     * @param message   原始内容
     * @param signature 待验证的签名
     * @return true 验证通过，false 验证失败
     */
    @Override
    public boolean verifyWeb(String message, String signature) {
        if (ObjUtil.isEmpty(message) || ObjUtil.isEmpty(signature)) {
            return false;
        }
        String key = SignSecretUtils.getSecret();
        return SignUtils.verifyWeb(message, signature, key);
    }

    /**
     * 添加签名密钥（仅本地缓存）
     *
     * @param secret 签名密钥
     */
    @Override
    public void addSecret(String secret) {
        AssertUtils.notEmpty(secret, "签名密钥不能为空");

        // 仅保存到本地，不涉及远程广播
        SignSecretUtils.saveSecret(secret);
        log.debug("签名密钥已缓存");
    }

    /**
     * 生成新的签名密钥
     *
     * @return 随机密钥字符串
     */
    @Override
    public String generateSecret() {
        // 委托给SignSecretManager生成密钥（仅Server端配置）
        // Client端保留此方法用于向后兼容，实际不应调用
        return IdUtil.fastSimpleUUID();
    }

    /**
     * 获取当前签名密钥
     *
     * @return 签名密钥
     * @throws IllegalStateException 当密钥未初始化时抛出异常
     */
    @Override
    public String getKey() {
        return SignSecretUtils.getSecret();
    }
}
