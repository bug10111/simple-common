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
 * Description: RSA非对称加密工具类
 * 公钥：表示公开的密钥
 * 私钥：表示私有的非公开密钥
 * <p>
 * 公钥加密，私钥解密
 * 私钥加密，公钥解密
 * 即可以互换使用
 * <p>安全说明：新建密钥对固定使用2048位；密钥材料禁止写日志/标准输出。</p>
 *
 * @author qty
 */
@Deprecated
@Slf4j
public class RsaUtils {

    /**
     * RSA 密钥最低安全位数
     */
    private static final int RSA_KEY_SIZE = 2048;

    private static final Map<String, RSA> map = new ConcurrentHashMap<>();

    private RsaUtils() {
    }

    /**
     * 获取新的RSA秘钥对（显式2048位，不再依赖Hutool默认1024位）
     */
    public static Map<KeyType, String> createRsaStr() {
        java.security.KeyPair keyPair = cn.hutool.crypto.KeyUtil.generateKeyPair("RSA", RSA_KEY_SIZE);
        RSA rsa = new RSA(keyPair.getPrivate(), keyPair.getPublic());
        Map<KeyType, String> keyTypeStringMap = new HashMap<>();
        keyTypeStringMap.put(KeyType.PrivateKey, rsa.getPrivateKeyBase64());
        keyTypeStringMap.put(KeyType.PublicKey, rsa.getPublicKeyBase64());
        return keyTypeStringMap;
    }

    /**
     * 添加RSA密钥对，允许添加单个公钥或者私钥进行加密、解密
     * <p>同名覆盖时打警告日志，提醒运维确认是否为误覆盖在用密钥。</p>
     *
     * @param name       名称
     * @param publicKey  公钥
     * @param privateKey 私钥
     */
    public static void saveRSA(String name, String publicKey, String privateKey) {
        if (map.containsKey(name)) {
            log.warn("RSA密钥对[{}]已存在，本次调用将覆盖在用密钥，请确认", name);
        }
        map.put(name, SecureUtil.rsa(privateKey, publicKey));
    }

    /**
     * 删除RSA密钥对
     *
     * @param name 名称
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
        AssertUtils.isTrue(rsa != null, "[{}]RSA密钥对未加载", name);
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
        try {
            return getRsa(name).encryptBase64(encryptStr, keyType);
        } catch (Exception e) {
            // 记录原始异常（含根因），不再静默吞掉异常细节
            log.error("RSA加密异常，name=[{}]", name, e);
            AssertUtils.error("加密异常，请检查密钥: {}", e.getMessage());
        }
        return "";
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
        try {
            return getRsa(name).decryptStr(encryptStr, keyType);
        } catch (Exception e) {
            log.error("RSA解密异常，name=[{}]", name, e);
            AssertUtils.error("解密异常，请检查参数和密钥: {}", e.getMessage());
        }
        return "";
    }
}
