package com.simple.common.auth.client.util;

import com.simple.common.core.utils.AssertUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
public class JJwtUtils {

    private static final AtomicReference<SecretKey> keyRef = new AtomicReference<>(null);

    /**
     * 密钥生成专用 CSPRNG：SecureRandom 线程安全，可静态复用
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * JWT 头部保留字段（RFC 7515/7517）：创建时强制剥离，
     * 防止调用方注入 alg/kid 等头造成算法混淆或验签歧义
     */
    private static final String[] RESERVED_HEADERS = {"alg", "typ", "kid", "jku", "jwk", "x5u", "x5t", "x5t#S256", "x5c", "cty"};

    /**
     * 添加密钥
     *
     * @param secret jwt密钥（≥64字符，HS512 要求；跨节点必须使用相同字符集推导密钥字节）
     */
    public static void saveSecret(String secret) {
        AssertUtils.notEmpty(secret, "jwt密钥不能为空");
        AssertUtils.isTrue(secret.length() >= 64, "jwt密钥长度至少为64位（HS512 要求），当前长度: " + secret.length());
        SecretKey newKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        keyRef.set(newKey);
    }

    /**
     * 获取新秘钥
     * <p>
     * 使用 CSPRNG（SecureRandom）生成 48 字节随机数并 Base64Url 编码为 64 字符密钥。
     * 禁止使用非加密随机源（如 RandomUtil.randomString）生成签名密钥。
     * </p>
     */
    public static String createSecret() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 生成令牌
     * <p>
     * header 中的保留字段（alg/typ/kid 等）会被剥离：签名算法一律由服务端配置决定，
     * 不接受调用方指定，防止算法混淆。
     * </p>
     *
     * @param header  头（业务自定义字段；可为 null）
     * @param payload 载荷
     * @return JWT token字符串
     */
    public static String createToken(Map<String, Object> header, Map<String, Object> payload) {
        var builder = Jwts.builder().claims(payload).signWith(getKey());
        if (header != null && !header.isEmpty()) {
            var headerBuilder = builder.header();
            header.forEach((name, value) -> {
                // 保留字段剥离，防算法混淆（alg 注入可致验签歧义）
                if (!isReservedHeader(name)) {
                    headerBuilder.add(name, value);
                }
            });
        }
        return builder.compact();
    }

    private static boolean isReservedHeader(String name) {
        if (name == null) {
            return true;
        }
        for (String reserved : RESERVED_HEADERS) {
            if (reserved.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析Claims，会自动验签
     * <p>
     * 校验要求：token 必须携带 exp 声明（缺失视为非法令牌，防止无过期令牌永久有效）。
     * </p>
     *
     * @param token token字符串
     * @return 解析后的载荷数据
     */
    public static Map<String, Object> verify(String token) {
        Map<String, Object> payload = Jwts.parser().verifyWith(getKey()).clockSkewSeconds(0).build()
                .parseSignedClaims(token).getPayload();
        AssertUtils.isTrue(payload.get(Claims.EXPIRATION) != null, "token缺少过期时间声明");
        return payload;
    }

    /**
     * 获取密钥
     *
     * @return JWT签名密钥
     * @throws IllegalStateException 当密钥未加载时抛出异常（密钥缺失是部署/编程错误，非业务异常）
     */
    public static SecretKey getKey() {
        SecretKey key = keyRef.get();
        if (key == null) {
            throw new IllegalStateException("请加载密钥");
        }
        return key;
    }
}
