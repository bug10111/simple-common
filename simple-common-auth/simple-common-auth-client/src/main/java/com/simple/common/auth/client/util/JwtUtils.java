package com.simple.common.auth.client.util;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.simple.common.core.utils.AssertUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * jwt工具封装
 *
 * @author 兄台丶请冷静
 */
@Slf4j
public class JwtUtils {

    private static String jwtSigner = null;

    /**
     * 获取新的JWTSigner秘钥
     */
    public static String createJWTSignerStr() {
        return RandomUtil.randomString(32);
    }

    /**
     * 添加JWTSigner密钥对，允许添加单个公钥或者私钥进行加密、解密
     *
     * @param JWTSigner JWTSigner密钥
     */
    public static void saveSecret(String JWTSigner) {
        jwtSigner = JWTSigner;
    }

    /**
     * 获取JWTSigner密钥对象
     */
    public static JWTSigner getJWTSigner() {
        AssertUtils.isTrue(jwtSigner != null, "JWTSigner密钥对未加载", "JWTSigner密钥对未加载,请使用saveJWTSigner进行初始化");
        return JWTSignerUtil.hs512(jwtSigner.getBytes());
    }

    /**
     * 创建jwt
     *
     * @param headers 头部信息
     * @param payload 有效载荷
     * @return token令牌
     */
    public static String createJwt(Map<String, Object> headers, Map<String, Object> payload) {
        return JWTUtil.createToken(headers, payload, getJWTSigner());
    }

    /**
     * 创建jwt
     *
     * @param payload 有效载荷
     * @return token令牌
     */
    public static String createJwt(Map<String, Object> payload) {
        return createJwt(null, payload);
    }

    /**
     * 验签，只会验证数据是否被篡改，不会验证过期等属性
     *
     * @param token Token
     * @return 是否有效
     */
    public static boolean verify(String token) {
        return JWTUtil.verify(token, getJWTSigner());
    }

    /**
     * 解析Token，并获取载荷信息
     *
     * @param Token 被解析的Token
     */
    public static Map<String, Object> getPayload(String Token) {
        return JWT.of(Token).getPayloads().getRaw();
    }
}
