package com.simple.common.core.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.SM2;
import cn.hutool.crypto.asymmetric.Sign;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA
 * Description: 签名工具类
 *
 * @author qty
 */
public class SignUtils {

    /**
     * 生成API签名字符串（安全实现）
     *
     * @param t          目标对象（仅包含业务参数）
     * @param excludeFields 需要排除的字段名（如secret、password等）
     * @return 按参数名排序的签名字符串
     */
    public static <T> String generateSignStr(T t, String... excludeFields) {
        // 1. 获取需要参与签名的字段（排除指定字段）
        Map<String, String> params = getSignableFields(t, excludeFields);

        // 2. 按参数名ASCII码升序排序
        List<Map.Entry<String, String>> sortedParams = new ArrayList<>(params.entrySet());
        sortedParams.sort(Map.Entry.comparingByKey());

        // 3. 拼接成字符串
        return sortedParams.stream().map(e -> e.getKey() + "=" + urlEncode(e.getValue())).collect(Collectors.joining("&"));
    }

    /**
     * 获取可用于签名的字段
     * @param t 对象
     * @param excludeFields 排除字段
     */
    private static <T> Map<String, String> getSignableFields(T t, String[] excludeFields) {
        Set<String> excludeSet = excludeFields == null ? Collections.emptySet() : new HashSet<>(Arrays.asList(excludeFields));

        Map<String, String> params = new TreeMap<>(); // 自动按key排序

        // 获取当前类的所有字段
        Class<?> clazz = t.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (excludeSet.contains(field.getName())) {
                    continue;
                }

                field.setAccessible(true);
                try {
                    Object value = field.get(t);
                    // 仅处理非空值（空值转为空字符串）
                    String strValue = value != null ? value.toString() : "";
                    params.put(field.getName(), strValue);
                } catch (Exception e) {
                    // 忽略反射异常，继续处理其他字段
                }
            }
            clazz = clazz.getSuperclass();
        }
        return params;
    }

    /**
     * 签名，适用于服务端之间请求
     *
     * 计算哈希：对数据使用安全哈希算法生成摘要（当前加密方式均内置自动哈希计算）
     * 私钥签名：使用发送者的私钥对哈希值进行加密（签名）
     * 公钥验证：接收者使用发送者的公钥解密签名，得到哈希值
     * 比对验证：接收者计算数据的哈希值，与解密得到的哈希值比对
     *
     * @param algorithm  算法
     * @param privateKey 私钥
     * @param data       数据
     * @return 签名数据
     */
    public static byte[] sign(CryptoUtil.AsymmetricAlgorithmType algorithm, PrivateKey privateKey, byte[] data) {
        try {
            if (algorithm.isRsa()) {
                Sign sign = new Sign(algorithm.getSignAlgorithm());
                sign.setPrivateKey(privateKey);
                return sign.sign(data);
            } else if (algorithm == CryptoUtil.AsymmetricAlgorithmType.SM2) {
                SM2 sm2 = new SM2(privateKey, null);
                return sm2.sign(data);
            } else {
                throw new RuntimeException("不支持的签名算法: " + algorithm);
            }
        } catch (Exception e) {
            throw new RuntimeException("签名失败: " + algorithm, e);
        }
    }

    /**
     * 验签
     *
     * @param algorithm 方式
     * @param publicKey 公钥
     * @param data      数据
     * @param signData  签名数据
     * @return 验证结果
     */
    public static boolean verify(CryptoUtil.AsymmetricAlgorithmType algorithm, PublicKey publicKey, byte[] data, byte[] signData) {
        try {
            if (algorithm.isRsa()) {
                Sign sign = new Sign(algorithm.getSignAlgorithm());
                sign.setPublicKey(publicKey);
                return sign.verify(data, signData);
            } else if (algorithm == CryptoUtil.AsymmetricAlgorithmType.SM2) {
                SM2 sm2 = new SM2(null, publicKey);
                return sm2.verify(data, signData);
            } else {
                throw new RuntimeException("不支持的验签算法: " + algorithm);
            }
        } catch (Exception e) {
            throw new RuntimeException("验签失败: " + algorithm, e);
        }
    }

    /**
     * 生成HMAC-SHA256签名，适用于web请求
     *
     * @param message   内容
     * @param secretKey 秘钥（可用没有“-”的uuid，建议每次请求更换秘钥，且和用户绑定）
     * @return 签名
     */
    public static String signWeb(String message, String secretKey) {
        return SecureUtil.hmacSha256(secretKey).digestBase64(message, true);
    }

    /**
     * 验证HMAC-SHA256签名
     *
     * @param message   内容
     * @param signature 签名字符串
     * @param secretKey 秘钥
     * @return 验签结果
     */
    public static boolean verifyWeb(String message, String signature, String secretKey) {
        String calculatedSignature = signWeb(message, secretKey);
        return calculatedSignature.equals(signature);
    }

    private static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20") // 保持与API服务端一致
                                      .replace("%21", "!").replace("%27", "'").replace("%28", "(").replace("%29", ")").replace("%7E", "~");
        } catch (Exception e) {
            throw new RuntimeException("URL编码失败", e);
        }
    }
}
