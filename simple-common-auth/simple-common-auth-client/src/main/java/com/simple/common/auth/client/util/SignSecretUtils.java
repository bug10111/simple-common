package com.simple.common.auth.client.util;

import com.simple.common.core.utils.AssertUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created with IntelliJ IDEA
 * 签名密钥管理工具类
 *
 * @author qty
 */
@Slf4j
public class SignSecretUtils {

    private static final AtomicReference<String> secretRef = new AtomicReference<>(null);

    /**
     * 保存签名密钥
     *
     * @param secret 签名密钥
     */
    public static void saveSecret(String secret) {
        AssertUtils.notEmpty(secret, "签名密钥不能为空");
        AssertUtils.isTrue(secret.length() >= 32, "签名密钥长度至少为32位，当前长度: " + secret.length());
        secretRef.set(secret);
    }

    /**
     * 获取当前签名密钥
     *
     * @return 签名密钥
     * @throws IllegalStateException 当密钥未加载时抛出异常
     */
    public static String getSecret() {
        String secret = secretRef.get();
        AssertUtils.isTrue(secret != null, "签名密钥未加载，请先调用saveSecret初始化");
        return secret;
    }
}
