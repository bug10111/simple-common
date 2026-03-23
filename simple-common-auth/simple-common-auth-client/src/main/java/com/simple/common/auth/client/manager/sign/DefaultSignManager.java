package com.simple.common.auth.client.manager.sign;

import cn.hutool.core.util.ObjUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.simple.common.auth.client.common.manager.sign.SignManager;
import com.simple.common.auth.client.common.properties.SignProperties;
import com.simple.common.cache.common.factory.LocalCacheFactory;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.IdUtils;
import com.simple.common.core.utils.SignUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultSignManager implements SignManager, InitializingBean {

    @Autowired
    private SignProperties signProperties;

    private Cache<String, String> string;

    //存储当前有效的签名密钥（全局）
    private volatile String currentKey;

    @Override
    public void generated() {
        //todo 可重写为从授权中心获取
        String newKey = IdUtils.getFastSimpleUUID();
        putKey(newKey);
        log.debug("HMAC-SHA256签名秘钥生成成功。");
    }

    @Override
    public void putKey(String key) {
        AssertUtils.notEmpty(key, "密钥不能为空");
        this.currentKey = key;
    }

    @Override
    public String getKey() {
        if (currentKey == null) {
            generated();
        }
        return currentKey;
    }

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

    @Override
    public void checkNonce(String nonce) {
        AssertUtils.notEmpty(nonce, "随机数不能为空");

        // 检查是否已使用
        String ifPresent = string.getIfPresent(nonce);
        AssertUtils.isTrue(ObjUtil.isEmpty(ifPresent), "非法请求");

        // 记录该 nonce（实际应设置过期时间，由清理线程负责）
        string.put(nonce, "1");
    }

    @Override
    public String signWeb(String message) {
        AssertUtils.notEmpty(message, "签名内容不能为空");
        return SignUtils.signWeb(message, currentKey);
    }

    @Override
    public boolean verifyWeb(String message, String signature) {
        if (ObjUtil.isEmpty(message) || ObjUtil.isEmpty(signature) ) {
            return false;
        }
        return SignUtils.verifyWeb(message, signature, currentKey);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        //初始化秘钥
        generated();

        //初始化nonce缓存
        LocalCacheFactory<String, String> factory = new LocalCacheFactory<>();
        string = factory.createCache("sign", config -> config.maximumSize(10000).expireAfterWrite(signProperties.getCacheTime()));
    }
}
