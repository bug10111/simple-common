package com.simple.common.auth.client.manager.sign;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.simple.common.auth.client.common.manager.sign.SignManager;
import com.simple.common.auth.client.common.properties.SignProperties;
import com.simple.common.auth.client.util.SignSecretUtils;
import com.simple.common.cache.common.factory.LocalCacheFactory;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.SignUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 默认签名管理器实现。
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultSignManager implements SignManager {

    @Autowired
    private SignProperties signProperties;

    /**
     * 专门用于 nonce 防重放的本地缓存（与业务缓存分离，确保高性能）。
     */
    private Cache<String, String> nonceCache;

    /**
     * 依赖注入完成后初始化 nonce 缓存。
     */
    @PostConstruct
    public void init() {
        // 初始化 nonce 缓存，容量和过期时间从配置读取
        this.nonceCache = LocalCacheFactory.getInstance().createCache("sign:nonce", spec -> spec.maximumSize(10000).expireAfterWrite(signProperties.getCacheTime()));
        log.info("签名管理器初始化完成，nonce 缓存过期时间: {}s", signProperties.getCacheTime());
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