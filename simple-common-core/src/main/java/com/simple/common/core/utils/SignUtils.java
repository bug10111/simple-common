package com.simple.common.core.utils;

import cn.hutool.crypto.asymmetric.SM2;
import cn.hutool.crypto.asymmetric.Sign;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Created with IntelliJ IDEA
 * Description: 签名工具类
 *
 * @author qty
 */
public class SignUtils {

    /**
     * 获取当前对象所有属性名和class，并生成字符串拼接
     *
     * @param t 目标对象
     */
    public static <T> String getSignStr(T t) {
        StringBuilder stringBuilder = new StringBuilder();
        append(t.getClass(), stringBuilder, t);
        return stringBuilder.toString();
    }

    /**
     * 拼接字符串
     *
     * @param aClass        class
     * @param stringBuilder 字符串
     */
    @SneakyThrows
    private static <T> void append(Class<?> aClass, StringBuilder stringBuilder, T t) {

        // 使用反射获取 base 的所有属性
        Field[] fields = aClass.getDeclaredFields();

        for (Field field : fields) {

            // 允许访问私有属性
            field.setAccessible(true);

            // 获取属性值
            Object value = field.get(t);
            if (value != null) {
                if (!stringBuilder.isEmpty()) {
                    stringBuilder.append("&");
                }
                stringBuilder.append(field.getName()).append("=").append(value);
            }
        }

        if (aClass.getSuperclass() != null) {
            append(aClass.getSuperclass(), stringBuilder, t);
        }
    }

    /**
     * 签名
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

}
