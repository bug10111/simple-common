package com.simple.common.core.utils;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.AsymmetricCrypto;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.SM2;
import cn.hutool.crypto.asymmetric.SignAlgorithm;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.Digester;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.DES;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
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
     * GCM 认证加密的认证标签长度（位）
     */
    private static final int GCM_TAG_LENGTH_BITS = 128;

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
            // 显式使用本地缓存的 BouncyCastle 提供者，与 SM4_GCM 路径保持一致，不依赖全局注册
            try {
                KeyGenerator generator = KeyGenerator.getInstance("SM4", getSm4GcmProvider().getName());
                generator.init(128, new SecureRandom());
                return generator.generateKey().getEncoded();
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("SM4 密钥生成需要 BouncyCastle 提供者，请确认 bcprov 依赖已引入", e);
            }
        }
        throw new RuntimeException("不支持的算法: " + algorithm);
    }

    /**
     * 生成对称加密秘钥（返回Base64字符串）
     *
     * @param algorithm 加密方式
     * @return Base64编码的秘钥字符串
     */
    public static String generateSymmetricKeyStr(SymmetricAlgorithmType algorithm) {
        return Base64Utils.encode(generateSymmetricKey(algorithm));
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
            // 生成随机初始向量：必须使用加密安全随机源（java.util.Random 可预测，会导致 GCM 密钥流复用）
            byte[] iv = new byte[algorithm.getIvLength()];
            new SecureRandom().nextBytes(iv);
            SecretKeySpec keySpec = new SecretKeySpec(key, algorithm.getAlgorithm());

            // GCM 模式使用标准 JCE 与 GCMParameterSpec，密文尾部自带认证标签
            if (algorithm.isGcm()) {
                byte[] encrypted = encryptGcm(algorithm, keySpec, iv, data);
                return ArrayUtil.addAll(iv, encrypted);
            }

            // CBC 模式沿用 Hutool 对称加密实现
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SymmetricCrypto crypto = createCbcCrypto(algorithm, keySpec, ivSpec);
            byte[] encrypted = crypto.encrypt(data);
            return ArrayUtil.addAll(iv, encrypted);
        } catch (Exception e) {
            throw new RuntimeException("对称加密失败: " + algorithm, e);
        }
    }

    /**
     * 对称加密（字符串版本）
     *
     * @param algorithm 加密方式
     * @param key       Base64编码的秘钥字符串
     * @param data      需要加密的字符串
     * @return Base64编码的加密后字符串
     */
    public static String encryptStr(SymmetricAlgorithmType algorithm, String key, String data) {
        byte[] keyBytes = Base64Utils.decode(key);
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = encrypt(algorithm, keyBytes, dataBytes);
        return Base64Utils.encode(encrypted);
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
            // 校验密文长度并拆分初始向量与密文体
            int ivLength = algorithm.getIvLength();
            // GCM 密文尾部还带有 128 位认证标签，短于该长度的输入必然无法通过认证，提前给出明确错误
            int minLength = ivLength + (algorithm.isGcm() ? GCM_TAG_LENGTH_BITS / 8 : 0);
            if (encryptedData.length < minLength) {
                throw new IllegalArgumentException("密文过短，无法提取IV"
                        + (algorithm.isGcm() ? "及认证标签" : ""));
            }
            byte[] iv = ArrayUtil.sub(encryptedData, 0, ivLength);
            byte[] cipherData = ArrayUtil.sub(encryptedData, ivLength, encryptedData.length);
            SecretKeySpec keySpec = new SecretKeySpec(key, algorithm.getAlgorithm());

            // GCM 模式由认证标签校验密文完整性，篡改密文将抛出异常
            if (algorithm.isGcm()) {
                return decryptGcm(algorithm, keySpec, iv, cipherData);
            }

            // CBC 模式沿用 Hutool 对称解密实现
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SymmetricCrypto crypto = createCbcCrypto(algorithm, keySpec, ivSpec);
            return crypto.decrypt(cipherData);
        } catch (Exception e) {
            throw new RuntimeException("对称解密失败: " + algorithm, e);
        }
    }

    /**
     * 对称解密（字符串版本）
     *
     * @param algorithm         解密方式
     * @param key               Base64编码的秘钥字符串
     * @param encryptedDataStr  Base64编码的加密字符串
     * @return 解密后的字符串
     */
    public static String decryptStr(SymmetricAlgorithmType algorithm, String key, String encryptedDataStr) {
        byte[] keyBytes = Base64Utils.decode(key);
        byte[] encryptedData = Base64Utils.decode(encryptedDataStr);
        byte[] decrypted = decrypt(algorithm, keyBytes, encryptedData);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * 使用 GCM 模式加密数据，返回密文与认证标签的拼接结果
     *
     * @param algorithm 加密方式
     * @param keySpec   密钥
     * @param iv        初始向量
     * @param data      明文
     * @return 密文与认证标签的拼接结果
     * @throws GeneralSecurityException 加密失败
     */
    private static byte[] encryptGcm(SymmetricAlgorithmType algorithm, SecretKeySpec keySpec, byte[] iv, byte[] data)
            throws GeneralSecurityException {
        Cipher cipher = createGcmCipher(algorithm);

        // GCM 必须使用 GCMParameterSpec 指定认证标签长度与初始向量
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return cipher.doFinal(data);
    }

    /**
     * 使用 GCM 模式解密数据，认证标签不匹配时抛出异常
     *
     * @param algorithm  解密方式
     * @param keySpec    密钥
     * @param iv         初始向量
     * @param cipherData 密文与认证标签的拼接结果
     * @return 明文
     * @throws GeneralSecurityException 解密失败或认证标签不匹配
     */
    private static byte[] decryptGcm(SymmetricAlgorithmType algorithm, SecretKeySpec keySpec, byte[] iv,
                                     byte[] cipherData) throws GeneralSecurityException {
        Cipher cipher = createGcmCipher(algorithm);

        // GCM 必须使用 GCMParameterSpec 指定认证标签长度与初始向量
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return cipher.doFinal(cipherData);
    }

    /**
     * 创建 GCM 模式 Cipher，SM4 算法显式使用 BouncyCastle 提供者
     *
     * @param algorithm 加解密方式
     * @return GCM 模式 Cipher
     * @throws GeneralSecurityException 当前运行环境不支持对应算法
     */
    private static Cipher createGcmCipher(SymmetricAlgorithmType algorithm) throws GeneralSecurityException {
        if (algorithm == SymmetricAlgorithmType.SM4_GCM) {
            return Cipher.getInstance(algorithm.getFullAlgorithm(), getSm4GcmProvider());
        }
        return Cipher.getInstance(algorithm.getFullAlgorithm());
    }

    /**
     * 获取 SM4_GCM 专用的 BouncyCastle 提供者，首次使用时创建并缓存，不依赖全局注册
     *
     * @return BouncyCastle 提供者
     */
    private static Provider getSm4GcmProvider() {
        try {
            return BouncyCastleProviderHolder.PROVIDER;
        } catch (Throwable e) {
            throw new IllegalStateException("SM4_GCM 需要 BouncyCastle 提供者，当前运行环境不可用，"
                    + "请确认 org.bouncycastle:bcprov-jdk15on 依赖已引入", e);
        }
    }

    /**
     * 创建 CBC 模式对称加解密器
     *
     * @param algorithm 加解密方式
     * @param keySpec   密钥
     * @param ivSpec    初始向量
     * @return 对称加解密器
     */
    private static SymmetricCrypto createCbcCrypto(SymmetricAlgorithmType algorithm, SecretKeySpec keySpec,
                                                   IvParameterSpec ivSpec) {
        return switch (algorithm) {
            case AES_CBC -> new AES(algorithm.getMode(), algorithm.getPadding(), keySpec, ivSpec);
            case DES_CBC -> new DES(algorithm.getMode(), algorithm.getPadding(), keySpec, ivSpec);
            case SM4_CBC -> new SymmetricCrypto(algorithm.getFullAlgorithm(), keySpec, ivSpec);
            default -> throw new IllegalArgumentException("非 CBC 模式不支持该方法: " + algorithm);
        };
    }

    /**
     * BouncyCastle 提供者持有者，避免每次加解密重复创建提供者
     */
    private static final class BouncyCastleProviderHolder {

        private static final Provider PROVIDER = new BouncyCastleProvider();
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
     * 非对称加密（字符串版本，公钥加密）
     *
     * @param algorithm     算法枚举
     * @param publicKeyPem  公钥PEM字符串
     * @param data          待加密字符串
     * @return Base64编码的密文字符串
     */
    public static String encryptStr(AsymmetricAlgorithmType algorithm, String publicKeyPem, String data) {
        PublicKey publicKey = getPublicKeyFromPem(publicKeyPem);
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = encrypt(algorithm, publicKey, dataBytes);
        return Base64Utils.encode(encrypted);
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
     * 非对称解密（字符串版本，私钥解密）
     *
     * @param algorithm         算法枚举
     * @param privateKeyPem     私钥PEM字符串
     * @param encryptedDataStr  Base64编码的密文字符串
     * @return 解密后的字符串
     */
    public static String decryptStr(AsymmetricAlgorithmType algorithm, String privateKeyPem, String encryptedDataStr) {
        PrivateKey privateKey = getPrivateKeyFromPem(privateKeyPem);
        byte[] encryptedData = Base64Utils.decode(encryptedDataStr);
        byte[] decrypted = decrypt(algorithm, privateKey, encryptedData);
        return new String(decrypted, StandardCharsets.UTF_8);
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
     * 哈希算法（字符串版本）
     *
     * @param algorithm 方式
     * @param data      需要计算的字符串
     * @return Base64编码的哈希结果字符串
     */
    public static String hashStr(HashAlgorithmType algorithm, String data) {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] hashResult = hash(algorithm, dataBytes);
        return Base64Utils.encode(hashResult);
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
    public static String getPublicKeyPem(PublicKey publicKey) {
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
    public static String getPrivateKeyPem(PrivateKey privateKey) {
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
     * <p>优先按 RSA 解析，失败时回退 EC（覆盖 SM2 密钥），保证 {@link #getPublicKeyPem(PublicKey)}
     * 导出的 SM2 公钥也能恢复。</p>
     */
    public static PublicKey getPublicKeyFromPem(String pem) {
        try {
            // 移除PEM头部和尾部
            String base64 = pem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");

            // Base64解码
            byte[] keyBytes = Base64Utils.decode(base64);

            // 创建公钥：先 RSA，失败回退 EC（SM2 公钥为 EC 编码）
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            try {
                return KeyFactory.getInstance("RSA").generatePublic(keySpec);
            } catch (InvalidKeySpecException e) {
                return KeyFactory.getInstance("EC").generatePublic(keySpec);
            }
        } catch (Exception e) {
            throw new RuntimeException("从PEM恢复公钥失败", e);
        }
    }

    /**
     * 从PEM字符串恢复指定算法的公钥
     *
     * @param algorithm 算法枚举，SM2 按 EC 解析，RSA 系列按 RSA 解析
     */
    public static PublicKey getPublicKeyFromPem(AsymmetricAlgorithmType algorithm, String pem) {
        if (algorithm == null || algorithm.isRsa()) {
            return getPublicKeyFromPem(pem);
        }
        String base64 = pem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
        byte[] keyBytes = Base64Utils.decode(base64);
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("EC").generatePublic(keySpec);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("从PEM恢复公钥失败: " + algorithm, e);
        }
    }

    /**
     * 从PEM字符串恢复私钥
     * <p>优先按 RSA 解析，失败时回退 EC（覆盖 SM2 密钥），保证 {@link #getPrivateKeyPem(PrivateKey)}
     * 导出的 SM2 私钥也能恢复。</p>
     */
    public static PrivateKey getPrivateKeyFromPem(String pem) {
        try {
            // 移除PEM头部和尾部
            String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");

            // Base64解码
            byte[] keyBytes = Base64Utils.decode(base64);

            // 创建私钥：先 RSA，失败回退 EC（SM2 私钥为 EC 编码）
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            try {
                return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
            } catch (InvalidKeySpecException e) {
                return KeyFactory.getInstance("EC").generatePrivate(keySpec);
            }
        } catch (Exception e) {
            throw new RuntimeException("从PEM恢复私钥失败", e);
        }
    }

    /**
     * 从PEM字符串恢复指定算法的私钥
     *
     * @param algorithm 算法枚举，SM2 按 EC 解析，RSA 系列按 RSA 解析
     */
    public static PrivateKey getPrivateKeyFromPem(AsymmetricAlgorithmType algorithm, String pem) {
        if (algorithm == null || algorithm.isRsa()) {
            return getPrivateKeyFromPem(pem);
        }
        String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
        byte[] keyBytes = Base64Utils.decode(base64);
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("EC").generatePrivate(keySpec);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("从PEM恢复私钥失败: " + algorithm, e);
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

        /**
         * 判断是否为 GCM 认证加密模式
         *
         * @return 是否为 GCM 认证加密模式
         */
        public boolean isGcm() {
            return this == AES_GCM || this == SM4_GCM;
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

        //加密方式
        private final String algorithm;

        //秘钥大小
        private final int keySize;

        //签名方式
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

    // ==================== 密码哈希方法（推荐使用BCrypt） ====================

    /**
     * 使用BCrypt算法对密码进行哈希
     * BCrypt是专门为密码存储设计的算法，自带盐值，抗彩虹表攻击
     *
     * @param password 明文密码
     * @return BCrypt哈希后的密码
     */
    public static String hashPassword(String password) {
        return DigestUtil.bcrypt(password);
    }

    /**
     * 验证密码是否与BCrypt哈希值匹配
     *
     * @param password     明文密码
     * @param hashedPassword BCrypt哈希后的密码
     * @return 是否匹配
     */
    public static boolean checkPassword(String password, String hashedPassword) {
        return DigestUtil.bcryptCheck(password, hashedPassword);
    }

}