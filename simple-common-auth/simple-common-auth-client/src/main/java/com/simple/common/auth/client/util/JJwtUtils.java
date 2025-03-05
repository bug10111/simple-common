package com.simple.common.auth.client.util;

import cn.hutool.core.util.RandomUtil;
import com.simple.common.core.utils.AssertUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Slf4j
public class JJwtUtils {

    private static SecretKey key = null;

    /**
     * 添加密钥
     *
     * @param secret jwt密钥
     */
    public static void saveSecret(String secret) {
        key = Keys.hmacShaKeyFor(secret.getBytes());
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
     */
    public static String createToken(Map<String, Object> header, Map<String, Object> payload) {
        return Jwts.builder().header().add(header).and().claims(payload).signWith(getKey()).compact();
    }

    /**
     * 验签
     *
     * @param token token
     */
    public static boolean isSigned(String token) {
        return Jwts.parser().verifyWith(getKey()).build().isSigned(token);
    }

    /**
     * 解析Claims，会自动验签
     *
     * @param token token
     */
    public static Map<String, Object> verify(String token) {
        return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload();
    }

    /**
     * 获取密钥
     */
    public static SecretKey getKey() {
        AssertUtils.isTrue(key != null, "请加载密钥");
        return key;
    }
}
