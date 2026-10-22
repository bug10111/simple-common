package com.simple.common.auth.client.util;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.simple.common.core.utils.AssertUtils;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created with IntelliJ IDEA
 * jwt工具封装
 *
 * @author qty
 */
@Slf4j
public class JwtUtils {

    private static final AtomicReference<String> jwtSignerRef = new AtomicReference<>(null);

    /**
     * 密钥生成专用 CSPRNG：SecureRandom 线程安全，可静态复用
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * JWT 头部保留字段（RFC 7515/7517）：创建时强制剥离，防算法混淆
     */
    private static final String[] RESERVED_HEADERS = {"alg", "typ", "kid", "jku", "jwk", "x5u", "x5t", "x5t#S256", "x5c", "cty"};

    /**
     * HS512 算法要求的密钥最小长度（RFC 7518：512 位 = 64 字节）
     */
    private static final int MIN_SECRET_LENGTH = 64;

    /**
     * 获取新的JWTSigner秘钥
     *
     * @return 64位随机字符串作为密钥（CSPRNG：SecureRandom + Base64Url，非加密随机源不可用于签名密钥）
     */
    public static String createJWTSignerStr() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 添加JWTSigner密钥对，允许添加单个公钥或者私钥进行加密、解密
     *
     * @param secret JWTSigner密钥字符串（≥64字符，HS512 要求）
     */
    public static void saveSecret(String secret) {
        // 密钥入库前校验：空/弱密钥安装即失败，避免运行期才以弱签名暴露
        AssertUtils.notEmpty(secret, "JWTSigner密钥不能为空");
        AssertUtils.isTrue(secret.trim().length() >= MIN_SECRET_LENGTH,
                "JWTSigner密钥长度至少为64位(HS512要求)，当前长度: " + secret.length());
        jwtSignerRef.set(secret.trim());
        log.debug("JWTSigner密钥已更新");
    }

    /**
     * 获取JWTSigner密钥对象
     *
     * @return JWT签名器实例
     * @throws IllegalStateException 当密钥未加载时抛出异常
     */
    public static JWTSigner getJWTSigner() {
        String jwtSigner = jwtSignerRef.get();
        if (jwtSigner == null) {
            throw new IllegalStateException("JWTSigner密钥对未加载，请使用saveSecret进行初始化");
        }
        // 显式 UTF-8：JVM 默认字符集随环境漂移（Windows GBK / Linux UTF-8），会导致跨节点验签失败
        return JWTSignerUtil.hs512(jwtSigner.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建jwt
     * <p>
     * headers 中的保留字段（alg/typ/kid 等）被剥离：签名算法由服务端配置决定，不接受调用方指定。
     * </p>
     *
     * @param headers 头部信息（业务自定义字段；可为 null）
     * @param payload 有效载荷
     * @return token令牌字符串
     */
    public static String createJwt(Map<String, Object> headers, Map<String, Object> payload) {
        if (headers == null || headers.isEmpty()) {
            return JWTUtil.createToken(payload, getJWTSigner());
        }
        java.util.Map<String, Object> safeHeaders = new java.util.HashMap<>(headers);
        for (String reserved : RESERVED_HEADERS) {
            safeHeaders.remove(reserved);
            safeHeaders.remove(reserved.toUpperCase());
            safeHeaders.remove(reserved.toLowerCase());
        }
        return JWTUtil.createToken(safeHeaders, payload, getJWTSigner());
    }

    /**
     * 创建jwt（无头部信息）
     *
     * @param payload 有效载荷
     * @return token令牌字符串
     */
    public static String createJwt(Map<String, Object> payload) {
        return createJwt(null, payload);
    }

    /**
     * 验签，只会验证数据是否被篡改，不会验证过期等属性
     *
     * @param token Token字符串（null/空白返回 false）
     * @return true表示验签通过，false表示验签失败
     */
    public static boolean verify(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        return JWTUtil.verify(token, getJWTSigner());
    }

    /**
     * 解析Token，并获取载荷信息
     * <p>
     * 先验签再返回载荷：不验签的解析结果完全可被攻击者伪造，禁止暴露未验签解析入口。
     * </p>
     *
     * @param token 被解析的Token字符串
     * @return 载荷数据的Map表示
     */
    public static Map<String, Object> getPayload(String token) {
        AssertUtils.isTrue(verify(token), "Token验签失败");
        return JWT.of(token).getPayloads().getRaw();
    }
}
