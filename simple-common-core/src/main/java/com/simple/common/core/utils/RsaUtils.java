package com.simple.common.core.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA
 * RSA非对称加密工具类
 * 公钥：表示公开的密钥
 * 私钥：表示私有的非公开密钥
 * <p>
 * 公钥加密，私钥解密
 * 私钥加密，公钥解密
 * 即可以互换使用
 *
 * @author 兄台丶请冷静
 */
@Slf4j
public class RsaUtils {

    private static final Map<String, RSA> map = new ConcurrentHashMap<>();

    /**
     * 获取新的RSA秘钥对
     */
    public static Map<KeyType, String> createRsaStr() {
        RSA rsa = new RSA();
        Map<KeyType, String> keyTypeStringMap = new HashMap<>();
        keyTypeStringMap.put(KeyType.PrivateKey, rsa.getPrivateKeyBase64());
        keyTypeStringMap.put(KeyType.PublicKey, rsa.getPublicKeyBase64());
        return keyTypeStringMap;
    }

    /**
     * 添加RSA密钥对，允许添加单个公钥或者私钥进行加密、解密
     *
     * @param name       名称
     * @param publicKey  公钥
     * @param privateKey 私钥
     */
    public static void saveRSA(String name, String publicKey, String privateKey) {
        map.put(name, SecureUtil.rsa(privateKey, publicKey));
    }

    /**
     * 删除RSA密钥对
     *
     * @param name       名称
     */
    public static void remRSA(String name) {
        map.remove(name);
    }

    /**
     * 获取RSA密钥对
     *
     * @param name 名称
     */
    public static RSA getRsa(String name) {
        RSA rsa = map.get(name);
        AssertUtils.isTrueParams(rsa != null, "[{}]RSA密钥对未加载", name);
        return rsa;
    }

    /**
     * 加密字符串
     *
     * @param name       名称
     * @param encryptStr 需要加密的字符串
     * @param keyType    加密用的密钥类型
     * @return 加密后的base64字符串
     */
    public static String encrypt(String name, String encryptStr, KeyType keyType) {
        String string = "";
        try {
            string = getRsa(name).encryptBase64(encryptStr, keyType);
        } catch (Exception e) {
            AssertUtils.error("加密异常，请检查密钥", e);
        }
        return string;
    }

    /**
     * 解密字符串
     *
     * @param name       名称
     * @param encryptStr 加密字符串
     * @param keyType    解密的密钥类型
     * @return 解密后的字符串
     */
    public static String decryptStr(String name, String encryptStr, KeyType keyType) {
        String string = "";
        try {
            string = getRsa(name).decryptStr(encryptStr, keyType);
        } catch (Exception e) {
            AssertUtils.error("解密异常，请检查参数和密钥", e.getMessage());
        }
        return string;
    }

    public static void main(String[] args) {
        Map<KeyType, String> rsaStr = createRsaStr();
        log.debug("私钥：[{}]", rsaStr.get(KeyType.PrivateKey));
        log.debug("公钥：[{}]", rsaStr.get(KeyType.PublicKey));
        saveRSA("demo", rsaStr.get(KeyType.PublicKey), rsaStr.get(KeyType.PrivateKey));
        System.out.println(decryptStr("demo", encrypt("demo", "你好！", KeyType.PrivateKey), KeyType.PublicKey));
    }
}
