package com.simple.common.auth.client.util;

import cn.hutool.core.util.RandomUtil;
import com.simple.common.core.utils.AssertUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
public class JJwtUtils {

    private static final AtomicReference<SecretKey> keyRef = new AtomicReference<>(null);

    /**
     * 添加密钥
     *
     * @param secret jwt密钥
     */
    public static void saveSecret(String secret) {
        SecretKey newKey = Keys.hmacShaKeyFor(secret.getBytes());
        keyRef.set(newKey);
    }

    /**
     * 获取新秘钥
     */
    public static String createSecret() {
        return RandomUtil.randomString(64);
    }

    /**
     * 生成令牌
     *
     * @param header  头
     * @param payload 载荷
     * @return JWT token字符串
     */
    public static String createToken(Map<String, Object> header, Map<String, Object> payload) {
        return Jwts.builder().header().add(header).and().claims(payload).signWith(getKey()).compact();
    }

    /**
     * 解析Claims，会自动验签
     *
     * @param token token字符串
     * @return 解析后的载荷数据
     */
    public static Map<String, Object> verify(String token) {
        return Jwts.parser().verifyWith(getKey()).clockSkewSeconds(0).build().parseSignedClaims(token).getPayload();
    }

    /**
     * 获取密钥
     *
     * @return JWT签名密钥
     * @throws IllegalStateException 当密钥未加载时抛出异常
     */
    public static SecretKey getKey() {
        SecretKey key = keyRef.get();
        AssertUtils.isTrue(key != null, "请加载密钥");
        return key;
    }
}
