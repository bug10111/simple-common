package com.simple.common.auth.client.manager.sign;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.manager.sign.SignManager;
import com.simple.common.auth.client.common.properties.SignProperties;
import com.simple.common.cache.common.factory.LocalCacheFactory;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.SignUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 默认签名管理器实现。
 * <p>
 * 修复内容：
 * 1. 移除错误的 LocalCacheFactory 泛型用法，改用正确 API 创建 nonce 缓存。
 * 2. 通过 @Qualifier 注入正确的 CacheManager。
 *
 * @author qty (修复版本)
 */
@Slf4j
@Component
public class DefaultSignManager implements SignManager, InitializingBean {

    private final CacheManager cacheManager;

    private final SignProperties signProperties;

    /**
     * 专门用于 nonce 防重放的本地缓存（与业务缓存分离，确保高性能）。
     */
    private final Cache<String, String> nonceCache;

    /**
     * 当前签名密钥（内存存储，重启后重新生成）。
     */
    private String currentKey;

    /**
     * 构造器注入，使用 @Qualifier 指定签名专用的 CacheManager。
     *
     * @param cacheManager   签名缓存管理器（由 ClientAuthConfig 创建）
     * @param signProperties 签名配置
     */
    public DefaultSignManager(@Qualifier("signCacheManager") CacheManager cacheManager, SignProperties signProperties) {
        this.cacheManager = cacheManager;
        this.signProperties = signProperties;
        // 初始化 nonce 缓存，容量和过期时间从配置读取
        this.nonceCache = LocalCacheFactory.getInstance().createCache("sign:nonce", spec -> spec.maximumSize(10000).expireAfterWrite(signProperties.getCacheTime()));
    }

    /**
     * Bean 初始化后自动生成密钥。
     */
    @Override
    public void afterPropertiesSet() {
        generated();
    }

    /**
     * 生成新的签名密钥（降级方案）。
     * <p>
     * 当前实现为本地 UUID 生成，仅适用于单机或开发环境。
     * 生产环境建议重写从配置文件、nacos、授权中心获取。
     */
    @Override
    public void generated() {
        String newKey = IdUtil.fastSimpleUUID();
        putKey(newKey);
        log.debug("HMAC-SHA256 签名密钥生成成功。");
    }

    /**
     * 设置当前使用的签名密钥。
     *
     * @param key 密钥字符串（不能为空）
     */
    @Override
    public void putKey(String key) {
        AssertUtils.notEmpty(key, "密钥不能为空");
        this.currentKey = key;
    }

    /**
     * 获取当前签名密钥。
     *
     * @return 密钥字符串，若未初始化则自动生成
     */
    @Override
    public String getKey() {
        if (currentKey == null) {
            generated();
        }
        return currentKey;
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
        long now = System.currentTimeMillis();
        long diff = Math.abs(now - ts);
        AssertUtils.isTrue(diff <= signProperties.getDefaultTimeWindowMs(), "请求已过期，时间差: " + diff + "ms");
        log.debug("时间戳校验通过，差值: {}ms", diff);
    }

    /**
     * 防重放校验：检查 nonce 是否已被使用。
     * <p>
     * 使用 Caffeine 缓存记录已使用的 nonce，过期时间与配置一致。
     *
     * @param nonce 随机数字符串
     */
    @Override
    public void checkNonce(String nonce) {
        AssertUtils.notEmpty(nonce, "随机数不能为空");
        String existing = nonceCache.getIfPresent(nonce);
        AssertUtils.isTrue(existing == null, "非法请求");
        nonceCache.put(nonce, "1");
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
        return SignUtils.signWeb(message, currentKey);
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
        return SignUtils.verifyWeb(message, signature, currentKey);
    }
}