package com.simple.common.core.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.*;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.Digester;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.DES;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Created with IntelliJ IDEA
 * Description: 加密工具类，支持对称、非对称、哈希算法
 * 安全说明：
 * 1. 推荐使用 AES_GCM、SM4_GCM 进行对称加密；RSA_OAEP 进行非对称加密；SHA256/SHA512/SM3 进行哈希。
 * 2. 旧系统兼容：保留了不安全算法（AES_CBC、DES_CBC、SM4_CBC、RSA_PKCS1、MD5），但使用时会打印警告日志，建议尽快迁移。
 *
 * @author qty
 */
@Slf4j
public class CryptoUtil {

    /**
     * 生成对称加密秘钥
     *
     * @param algorithm 加密方式
     */
    public static byte[] generateSymmetricKey(SymmetricAlgorithmType algorithm) {
        String alg = algorithm.getAlgorithm();
        if ("AES".equals(alg)) {
            return SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue()).getEncoded();
        } else if ("DES".equals(alg)) {
            return SecureUtil.generateKey(SymmetricAlgorithm.DES.getValue()).getEncoded();
        } else if ("SM4".equals(alg)) {
            return SecureUtil.generateKey("SM4").getEncoded();
        }
        throw new RuntimeException("不支持的算法: " + algorithm);
    }

    /**
     * 生成非对称密钥对
     *
     * @param algorithm 算法枚举
     * @return 密钥对
     */
    public static KeyPair generateKeyPair(AsymmetricAlgorithmType algorithm) {
        try {
            if (algorithm.isRsa()) {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(algorithm.getKeySize());
                return keyGen.generateKeyPair();
            } else if (algorithm == AsymmetricAlgorithmType.SM2) {
                SM2 sm2 = SmUtil.sm2();
                PrivateKey privateKey = sm2.getPrivateKey();
                PublicKey publicKey = sm2.getPublicKey();
                return new KeyPair(publicKey, privateKey);
            } else {
                throw new RuntimeException("不支持的算法: " + algorithm);
            }
        } catch (Exception e) {
            throw new RuntimeException("生成非对称密钥对失败: " + algorithm, e);
        }
    }

    /**
     * 对称加密
     *
     * @param algorithm 加密方式
     * @param key       秘钥
     * @param data      需要加密的数据
     * @return 加密后的数据
     */
    public static byte[] encrypt(SymmetricAlgorithmType algorithm, byte[] key, byte[] data) {
        // 旧系统兼容：不安全算法仅警告，不阻断
        if (algorithm.isUnsafe()) {
            log.warn("使用了不安全的对称加密算法: {}，请尽快迁移至 AES_GCM 或 SM4_GCM", algorithm);
        }

        try {
            byte[] iv = RandomUtil.randomBytes(algorithm.getIvLength());
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec keySpec = new SecretKeySpec(key, algorithm.getAlgorithm());

            SymmetricCrypto crypto = switch (algorithm) {
                case AES_CBC -> new AES(algorithm.getMode(), algorithm.getPadding(), keySpec, ivSpec);
                case AES_GCM, SM4_GCM, SM4_CBC -> new SymmetricCrypto(algorithm.getFullAlgorithm(), keySpec, ivSpec);
                case DES_CBC -> new DES(algorithm.getMode(), algorithm.getPadding(), keySpec, ivSpec);
            };

            byte[] encrypted = crypto.encrypt(data);
            return ArrayUtil.addAll(iv, encrypted);
        } catch (Exception e) {
            throw new RuntimeException("对称加密失败: " + algorithm, e);
        }
    }

    /**
     * 对称解密
     *
     * @param algorithm     解密方式
     * @param key           秘钥
     * @param encryptedData 需要解密的数据
     * @return 解密后的数据
     */
    public static byte[] decrypt(SymmetricAlgorithmType algorithm, byte[] key, byte[] encryptedData) {
        // 旧系统兼容：不安全算法仅警告，不阻断
        if (algorithm.isUnsafe()) {
            log.warn("使用了不安全的对称解密算法: {}，请尽快迁移至 AES_GCM 或 SM4_GCM", algorithm);
        }

        try {
            int ivLength = algorithm.getIvLength();
            if (encryptedData.length < ivLength) {
                throw new IllegalArgumentException("密文过短，无法提取IV");
            }
            byte[] iv = ArrayUtil.sub(encryptedData, 0, ivLength);
            byte[] cipherData = ArrayUtil.sub(encryptedData, ivLength, encryptedData.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec keySpec = new SecretKeySpec(key, algorithm.getAlgorithm());

            SymmetricCrypto crypto = switch (algorithm) {
                case AES_CBC -> new AES(algorithm.getMode(), algorithm.getPadding(), keySpec, ivSpec);
                case AES_GCM, SM4_GCM, SM4_CBC -> new SymmetricCrypto(algorithm.getFullAlgorithm(), keySpec, ivSpec);
                case DES_CBC -> new DES(algorithm.getMode(), algorithm.getPadding(), keySpec, ivSpec);
            };

            return crypto.decrypt(cipherData);
        } catch (Exception e) {
            throw new RuntimeException("对称解密失败: " + algorithm, e);
        }
    }

    /**
     * 非对称加密（公钥加密）
     *
     * @param algorithm 算法枚举
     * @param publicKey 公钥
     * @param data      待加密数据
     * @return 密文
     */
    public static byte[] encrypt(AsymmetricAlgorithmType algorithm, PublicKey publicKey, byte[] data) {
        // 旧系统兼容：不安全算法仅警告，不阻断
        if (algorithm.isUnsafeForEncryption()) {
            log.warn("使用了不安全的非对称加密算法: {}，请尽快迁移至 RSA_OAEP", algorithm);
        }

        try {
            if (algorithm.isRsa()) {
                AsymmetricCrypto crypto = new AsymmetricCrypto(algorithm.getAlgorithm(), null, publicKey);
                return crypto.encrypt(data, KeyType.PublicKey);
            } else if (algorithm == AsymmetricAlgorithmType.SM2) {
                SM2 sm2 = new SM2(null, publicKey);
                return sm2.encrypt(data);
            } else {
                throw new RuntimeException("不支持的算法: " + algorithm);
            }
        } catch (Exception e) {
            throw new RuntimeException("非对称加密失败: " + algorithm, e);
        }
    }

    /**
     * 非对称解密（私钥解密）
     *
     * @param algorithm     算法枚举
     * @param privateKey    私钥
     * @param encryptedData 密文
     * @return 明文
     */
    public static byte[] decrypt(AsymmetricAlgorithmType algorithm, PrivateKey privateKey, byte[] encryptedData) {
        try {
            if (algorithm.isRsa()) {
                AsymmetricCrypto crypto = new AsymmetricCrypto(algorithm.getAlgorithm(), privateKey, null);
                return crypto.decrypt(encryptedData, KeyType.PrivateKey);
            } else if (algorithm == AsymmetricAlgorithmType.SM2) {
                SM2 sm2 = new SM2(privateKey, null);
                return sm2.decrypt(encryptedData);
            } else {
                throw new RuntimeException("不支持的算法: " + algorithm);
            }
        } catch (Exception e) {
            throw new RuntimeException("非对称解密失败: " + algorithm, e);
        }
    }

    /**
     * 哈希算法
     *
     * @param algorithm 方式
     * @param data      需要计算的数据
     * @return 计算结果
     */
    public static byte[] hash(HashAlgorithmType algorithm, byte[] data) {
        // 旧系统兼容：MD5仅警告，不阻断
        if (algorithm == HashAlgorithmType.MD5) {
            log.warn("使用了不安全的哈希算法: MD5，请尽快迁移至 SHA256 或 SM3");
        }

        try {
            Digester digester = DigestUtil.digester(algorithm.getAlgorithm());
            return digester.digest(data);
        } catch (Exception e) {
            throw new RuntimeException("哈希计算失败: " + algorithm, e);
        }
    }

    /**
     * 从PEM字符串恢复公钥和私钥
     *
     * @param publicKeyPem  公钥PEM字符串
     * @param privateKeyPem 私钥PEM字符串
     * @return 密钥对
     */
    public static KeyPair restoreKeyPair(String publicKeyPem, String privateKeyPem) {
        PublicKey publicKey = getPublicKeyFromPem(publicKeyPem);
        PrivateKey privateKey = getPrivateKeyFromPem(privateKeyPem);
        return new KeyPair(publicKey, privateKey);
    }

    /**
     * 将公钥转换为PEM格式字符串
     */
    private static String getPublicKeyPem(PublicKey publicKey) {
        try {
            // 获取X.509编码格式
            byte[] encoded = publicKey.getEncoded();
            String base64 = Base64Utils.encode(encoded);
            return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n" + "-----END PUBLIC KEY-----";
        } catch (Exception e) {
            throw new RuntimeException("获取公钥PEM格式失败", e);
        }
    }

    /**
     * 将私钥转换为PEM格式字符串
     */
    private static String getPrivateKeyPem(PrivateKey privateKey) {
        try {
            // 获取PKCS#8编码格式
            byte[] encoded = privateKey.getEncoded();
            String base64 = Base64Utils.encode(encoded);
            return "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n" + "-----END PRIVATE KEY-----";
        } catch (Exception e) {
            throw new RuntimeException("获取私钥PEM格式失败", e);
        }
    }

    /**
     * 从PEM字符串恢复公钥
     */
    private static PublicKey getPublicKeyFromPem(String pem) {
        try {
            // 移除PEM头部和尾部
            String base64 = pem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");

            // Base64解码
            byte[] keyBytes = Base64.decode(base64);

            // 创建公钥
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("从PEM恢复公钥失败", e);
        }
    }

    /**
     * 从PEM字符串恢复私钥
     */
    private static PrivateKey getPrivateKeyFromPem(String pem) {
        try {
            // 移除PEM头部和尾部
            String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");

            // Base64解码
            byte[] keyBytes = Base64.decode(base64);

            // 创建私钥
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("从PEM恢复私钥失败", e);
        }
    }

    @Getter
    @AllArgsConstructor
    public enum SymmetricAlgorithmType {

        // 不安全：CBC模式无认证，易受Padding Oracle攻击（仅用于旧系统兼容）
        AES_CBC("AES", "CBC", "PKCS5Padding", 16),

        // 不安全：DES密钥长度仅56位，易被暴力破解（仅用于旧系统兼容）
        DES_CBC("DES", "CBC", "PKCS5Padding", 8),

        // 不安全：CBC模式无认证，易受Padding Oracle攻击（仅用于旧系统兼容）
        SM4_CBC("SM4", "CBC", "PKCS5Padding", 16),

        // 安全：GCM模式自带认证加密（AEAD）
        AES_GCM("AES", "GCM", "NoPadding", 12),

        // 安全：GCM模式自带认证加密（AEAD）
        SM4_GCM("SM4", "GCM", "NoPadding", 12);

        private final String algorithm;

        private final String mode;

        private final String padding;

        private final int ivLength;

        public String getFullAlgorithm() {
            return algorithm + "/" + mode + "/" + padding;
        }

        // 是否为不安全算法（用于日志警告）
        public boolean isUnsafe() {
            return this == AES_CBC || this == DES_CBC || this == SM4_CBC;
        }
    }

    /**
     * 非对称加密枚举
     */
    @Getter
    @AllArgsConstructor
    public enum AsymmetricAlgorithmType {

        // 不安全：PKCS#1 v1.5填充易受Bleichenbacher攻击（仅用于旧系统兼容）
        RSA_PKCS1("RSA/ECB/PKCS1Padding", 2048, SignAlgorithm.SHA256withRSA),

        // 安全（签名）/ 不安全（加密）：加密使用PKCS1Padding不安全，但签名使用PSS安全（仅用于旧系统兼容）
        RSA_PSS("RSA/ECB/PKCS1Padding", 2048, SignAlgorithm.SHA256withRSA_PSS),

        // 安全：OAEP填充（现代标准，抵御Bleichenbacher攻击）
        RSA_OAEP("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", 2048, SignAlgorithm.SHA256withRSA),

        // 安全：国密标准，安全可靠
        SM2("SM2", 256, null);

        private final String algorithm;

        private final int keySize;

        private final SignAlgorithm signAlgorithm;

        /**
         * 判断是否为 RSA 算法（包括 PKCS1、PSS、OAEP）
         */
        public boolean isRsa() {
            return this == RSA_PKCS1 || this == RSA_PSS || this == RSA_OAEP;
        }

        // 是否为不安全算法（仅针对加密操作）
        public boolean isUnsafeForEncryption() {
            return this == RSA_PKCS1 || this == RSA_PSS;
        }
    }

    @Getter
    @AllArgsConstructor
    public enum HashAlgorithmType {

        // 不安全：MD5已存在碰撞攻击（仅用于旧系统兼容）
        MD5("MD5"),

        // 安全：标准安全哈希算法
        SHA256("SHA-256"),

        // 安全：标准安全哈希算法
        SHA512("SHA-512"),

        // 安全：国密标准哈希算法
        SM3("SM3");

        private final String algorithm;
    }

    public static void main(String[] args) {
        // 对称加密测试（AES/GCM - 安全版本）
        System.out.println("========== 对称加密（AES/GCM） ==========");
        SymmetricAlgorithmType symAlgo = SymmetricAlgorithmType.AES_GCM;
        byte[] symKey = generateSymmetricKey(symAlgo);
        String plain = "Hello, 安全对称加密!";
        byte[] encrypted = encrypt(symAlgo, symKey, plain.getBytes());
        byte[] decrypted = decrypt(symAlgo, symKey, encrypted);
        System.out.println("原始: " + plain);
        System.out.println("加密后(Base64): " + Base64Utils.encode(encrypted));
        System.out.println("解密后: " + new String(decrypted));

        // RSA OAEP 非对称加密测试（安全）
        System.out.println("\n========== RSA OAEP 非对称加密 ==========");
        AsymmetricAlgorithmType rsaOaep = AsymmetricAlgorithmType.RSA_OAEP;
        KeyPair rsaOaepPair = generateKeyPair(rsaOaep);
        String rsaPlain = "RSA OAEP 加密测试";
        byte[] rsaEnc = encrypt(rsaOaep, rsaOaepPair.getPublic(), rsaPlain.getBytes());
        byte[] rsaDec = decrypt(rsaOaep, rsaOaepPair.getPrivate(), rsaEnc);
        System.out.println("原始: " + rsaPlain);
        System.out.println("加密后(Base64): " + Base64Utils.encode(rsaEnc));
        System.out.println("解密后: " + new String(rsaDec));

        // SM2 非对称加密测试
        System.out.println("\n========== SM2 非对称加密 ==========");
        AsymmetricAlgorithmType sm2Algo = AsymmetricAlgorithmType.SM2;
        KeyPair sm2KeyPair = generateKeyPair(sm2Algo);
        String sm2Plain = "SM2 安全加密测试";
        byte[] sm2Enc = encrypt(sm2Algo, sm2KeyPair.getPublic(), sm2Plain.getBytes());
        byte[] sm2Dec = decrypt(sm2Algo, sm2KeyPair.getPrivate(), sm2Enc);
        System.out.println("原始: " + sm2Plain);
        System.out.println("加密后(Base64): " + Base64Utils.encode(sm2Enc));
        System.out.println("解密后: " + new String(sm2Dec));

        // 哈希测试（安全算法）
        System.out.println("\n========== 安全哈希算法 ==========");
        String hashData = "Hello World";
        System.out.println("SHA256: " + Base64Utils.encode(hash(HashAlgorithmType.SHA256, hashData.getBytes())));
        System.out.println("SM3: " + Base64Utils.encode(hash(HashAlgorithmType.SM3, hashData.getBytes())));

        // 演示旧系统兼容（会打印警告日志）
        System.out.println("\n========== 旧系统兼容示例（会打印警告） ==========");
        SymmetricAlgorithmType unsafeSym = SymmetricAlgorithmType.AES_CBC;
        byte[] unsafeKey = generateSymmetricKey(unsafeSym);
        byte[] unsafeEnc = encrypt(unsafeSym, unsafeKey, "旧数据".getBytes());
        byte[] unsafeDec = decrypt(unsafeSym, unsafeKey, unsafeEnc);
        System.out.println("旧系统解密成功: " + new String(unsafeDec));

        AsymmetricAlgorithmType unsafeAsym = AsymmetricAlgorithmType.RSA_PKCS1;
        KeyPair unsafeKeyPair = generateKeyPair(unsafeAsym);
        byte[] unsafeAsymEnc = encrypt(unsafeAsym, unsafeKeyPair.getPublic(), "旧RSA数据".getBytes());
        byte[] unsafeAsymDec = decrypt(unsafeAsym, unsafeKeyPair.getPrivate(), unsafeAsymEnc);
        System.out.println("旧系统RSA解密成功: " + new String(unsafeAsymDec));

        byte[] md5Hash = hash(HashAlgorithmType.MD5, "旧数据".getBytes());
        System.out.println("MD5哈希: " + Base64Utils.encode(md5Hash));
    }
}