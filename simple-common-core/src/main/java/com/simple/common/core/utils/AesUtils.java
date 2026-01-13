package com.simple.common.core.utils;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA
 * AES对称加密工具类
 *
 * @author qty
 */
public class AesUtils {

    private static final Map<String, AES> map = new ConcurrentHashMap<>();

    /**
     * 获取新的AES秘钥
     */
    public static String createAESStr() {
        return RandomUtil.randomString(32);
    }

    /**
     * 添加AES密钥对，允许添加单个公钥或者私钥进行加密、解密
     *
     * @param name 名称
     * @param aes  aes密钥
     */
    public static void saveAES(String name, String aes) {
        map.put(name, SecureUtil.aes(aes.getBytes()));
    }

    /**
     * 获取AES密钥对
     *
     * @param name 名称
     */
    public static AES getAES(String name) {
        AES AES = map.get(name);
        AssertUtils.isTrueParams(AES != null, "{}AES密钥对未加载", name);
        return AES;
    }

    /**
     * 加密字符串
     *
     * @param name       名称
     * @param encryptStr 需要加密的字符串
     * @return 加密后的base64字符串
     */
    public static String encrypt(String name, String encryptStr) {
        return getAES(name).encryptBase64(encryptStr);
    }

    /**
     * 直接加密字符串
     *
     * @param aes        密钥
     * @param encryptStr 需要加密的字符串
     * @return 加密后的base64字符串
     */
    public static String directEncrypt(String aes, String encryptStr) {
        return SecureUtil.aes(aes.getBytes()).encryptBase64(encryptStr);
    }

    /**
     * 解密字符串
     *
     * @param name       名称
     * @param encryptStr 加密字符串
     * @return 解密后的字符串
     */
    public static String decryptStr(String name, String encryptStr) {
        String string = "";
        try {
            string = getAES(name).decryptStr(encryptStr);
        } catch (Exception e) {
            AssertUtils.error("解密异常，请检查密钥", e);
        }
        return string;
    }

    public static void main(String[] args) {
        String aesStr = createAESStr();
        System.out.println(aesStr);
        saveAES("demo", aesStr);
        System.out.println(getAES("demo").decryptStr(getAES("demo").encryptBase64("你好！")));
    }
}
