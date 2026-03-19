//package com.simple.common.core.utils;
//
//import lombok.extern.slf4j.Slf4j;
//
//import org.bouncycastle.asn1.gm.GMNamedCurves;
//import org.bouncycastle.asn1.x9.X9ECParameters;
//import org.bouncycastle.crypto.digests.SM3Digest;
//import org.bouncycastle.crypto.engines.SM2Engine;
//import org.bouncycastle.crypto.engines.SM4Engine;
//import org.bouncycastle.crypto.modes.CBCBlockCipher;
//import org.bouncycastle.crypto.paddings.PKCS7Padding;
//import org.bouncycastle.crypto.paddings.BlockCipherPadding;
//import org.bouncycastle.crypto.params.*;
//import org.bouncycastle.jce.provider.BouncyCastleProvider;
//import org.bouncycastle.math.ec.ECPoint;
//import org.bouncycastle.util.encoders.Hex;
//
//import javax.crypto.Cipher;
//import javax.crypto.KeyGenerator;
//import javax.crypto.SecretKey;
//import javax.crypto.spec.IvParameterSpec;
//import javax.crypto.spec.SecretKeySpec;
//import java.security.*;
//import java.security.spec.ECGenParameterSpec;
//import java.security.spec.PKCS8EncodedKeySpec;
//import java.security.spec.X509EncodedKeySpec;
//import java.util.Arrays;
///**
// * Created with IntelliJ IDEA
// *
// * @author qty
// */
//@Slf4j
//public class SmUtil {
//
//    // BouncyCastle Provider名称（国密算法必需）
//    private static final String BC_PROVIDER = "BC";
//    // SM2曲线名称（国密标准曲线）
//    private static final String SM2_CURVE_NAME = "sm2p256v1";
//    // SM4密钥长度（128位）
//    private static final int SM4_KEY_SIZE = 128;
//    // SM4 IV长度（16字节）
//    private static final int SM4_IV_SIZE = 16;
//    // 默认加密格式：C1C3C2（国密标准），部分厂商要求C1C2C3
//    private static final boolean DEFAULT_CIPHER_FORMAT_C1C3C2 = true;
//
//    static {
//        // 注册BouncyCastle Provider（线程安全）
//        if (Security.getProvider(BC_PROVIDER) == null) {
//            Security.addProvider(new BouncyCastleProvider());
//            log.info("BouncyCastle Provider已注册，支持国密算法");
//        }
//    }
//
//    // ====================== SM2 非对称加密（密钥交换/数字签名） ======================
//
//    /**
//     * 生成SM2密钥对（公钥+私钥）
//     *
//     * ⚠️ 安全警告：
//     * 1. 生成的私钥必须通过KMS/HSM安全存储，禁止硬编码/日志输出
//     * 2. 生产环境建议使用硬件加密机生成密钥
//     * 3. 私钥对象使用后应立即调用clearPrivateKey()清零
//     *
//     * @return KeyPair 包含公钥(PublicKey)和私钥(PrivateKey)
//     * @throws RuntimeException 密钥生成失败
//     */
//    public static KeyPair generateSm2KeyPair() {
//        try {
//            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("EC", BC_PROVIDER);
//            // 使用国密标准曲线
//            ECGenParameterSpec sm2Spec = new ECGenParameterSpec(SM2_CURVE_NAME);
//            keyPairGen.initialize(sm2Spec, new SecureRandom());
//            return keyPairGen.generateKeyPair();
//        } catch (Exception e) {
//            log.error("SM2密钥对生成失败", e);
//            throw new RuntimeException("SM2密钥对生成失败", e);
//        }
//    }
//
//    /**
//     * SM2加密（使用公钥加密）
//     *
//     * 📌 标准说明：
//     * - 默认输出C1C3C2格式（符合GM/T 0003-2012）
//     * - C1: 椭圆曲线点（65字节）
//     * - C3: SM3摘要（32字节）
//     * - C2: 密文数据
//     * - 部分厂商（如某些硬件加密机）要求C1C2C3格式，通过cipherFormatC1C3C2参数切换
//     *
//     * @param data 待加密的原始数据（字节数组）
//     * @param publicKey 公钥（X509EncodedKeySpec格式）
//     * @param cipherFormatC1C3C2 true=C1C3C2(标准), false=C1C2C3(兼容模式)
//     * @return 加密后的字节数组（HEX字符串建议使用Hex.toHexString()转换）
//     * @throws RuntimeException 加密失败
//     */
//    public static byte[] sm2Encrypt(byte[] data, PublicKey publicKey, boolean cipherFormatC1C3C2) {
//        if (data == null || data.length == 0) {
//            throw new IllegalArgumentException("加密数据不能为空");
//        }
//        if (publicKey == null) {
//            throw new IllegalArgumentException("公钥不能为空");
//        }
//
//        try {
//            // 使用BouncyCastle原生API（避免Cipher类兼容性问题）
//            SM2Engine engine = new SM2Engine();
//            ECPublicKeyParameters pubKeyParams = (ECPublicKeyParameters)
//                            PublicKeyFactory.createKey(publicKey.getEncoded());
//
//            engine.init(true, new ParametersWithRandom(pubKeyParams, new SecureRandom()));
//            byte[] ciphertext = engine.processBlock(data, 0, data.length);
//
//            // 格式转换：BouncyCastle默认C1C2C3，需转换为C1C3C2（国密标准）
//            if (cipherFormatC1C3C2) {
//                return convertCipherText(ciphertext, true);
//            }
//            return ciphertext;
//        } catch (Exception e) {
//            log.error("SM2加密失败", e);
//            throw new RuntimeException("SM2加密失败", e);
//        }
//    }
//
//    /**
//     * SM2解密（使用私钥解密）
//     *
//     * ⚠️ 安全操作：
//     * 1. 解密后立即清零私钥内存（调用clearPrivateKey）
//     * 2. 敏感数据处理后建议调用Arrays.fill(ciphertext, (byte)0)清零
//     *
//     * @param ciphertext 密文（字节数组，格式需与加密时一致）
//     * @param privateKey 私钥（PKCS8EncodedKeySpec格式）
//     * @param cipherFormatC1C3C2 true=C1C3C2(标准), false=C1C2C3(兼容模式)
//     * @return 解密后的原始数据
//     * @throws RuntimeException 解密失败
//     */
//    public static byte[] sm2Decrypt(byte[] ciphertext, PrivateKey privateKey, boolean cipherFormatC1C3C2) {
//        if (ciphertext == null || ciphertext.length == 0) {
//            throw new IllegalArgumentException("密文不能为空");
//        }
//        if (privateKey == null) {
//            throw new IllegalArgumentException("私钥不能为空");
//        }
//
//        try {
//            // 格式转换：若密文为C1C3C2，需转回C1C2C3供BouncyCastle处理
//            if (cipherFormatC1C3C2) {
//                ciphertext = convertCipherText(ciphertext, false);
//            }
//
//            SM2Engine engine = new SM2Engine();
//            ECPrivateKeyParameters privKeyParams = (ECPrivateKeyParameters)
//                            PrivateKeyFactory.createKey(privateKey.getEncoded());
//
//            engine.init(false, privKeyParams);
//            return engine.processBlock(ciphertext, 0, ciphertext.length);
//        } catch (Exception e) {
//            log.error("SM2解密失败", e);
//            throw new RuntimeException("SM2解密失败", e);
//        } finally {
//            // 安全建议：调用方应在使用后清零私钥（此处无法直接操作PrivateKey对象）
//            // 建议：私钥使用byte[]临时存储时，调用clearByteArray()
//        }
//    }
//
//    /**
//     * SM2签名（使用私钥签名）
//     *
//     * 📌 签名流程：
//     * 1. 对原始数据计算SM3摘要
//     * 2. 使用私钥对摘要进行SM2签名
//     * 3. 输出DER编码的签名值（符合X.509标准）
//     *
//     * @param data 待签名的原始数据
//     * @param privateKey 私钥
//     * @return 签名值（DER编码，通常转为Base64传输）
//     * @throws RuntimeException 签名失败
//     */
//    public static byte[] sm2Sign(byte[] data, PrivateKey privateKey) {
//        if (data == null || data.length == 0) {
//            throw new IllegalArgumentException("签名数据不能为空");
//        }
//        if (privateKey == null) {
//            throw new IllegalArgumentException("私钥不能为空");
//        }
//
//        try {
//            Signature signature = Signature.getInstance("SM3withSM2", BC_PROVIDER);
//            signature.initSign(privateKey, new SecureRandom());
//            signature.update(data);
//            return signature.sign();
//        } catch (Exception e) {
//            log.error("SM2签名失败", e);
//            throw new RuntimeException("SM2签名失败", e);
//        }
//    }
//
//    /**
//     * SM2验签（使用公钥验证签名）
//     *
//     * @param data 原始数据
//     * @param signature 签名值（DER编码）
//     * @param publicKey 公钥
//     * @return true=验证通过，false=验证失败
//     */
//    public static boolean sm2Verify(byte[] data, byte[] signature, PublicKey publicKey) {
//        if (data == null || signature == null || publicKey == null) {
//            return false;
//        }
//        try {
//            Signature verifier = Signature.getInstance("SM3withSM2", BC_PROVIDER);
//            verifier.initVerify(publicKey);
//            verifier.update(data);
//            return verifier.verify(signature);
//        } catch (Exception e) {
//            log.error("SM2验签异常", e);
//            return false;
//        }
//    }
//
//    // ====================== SM4 对称加密（大数据加密） ======================
//
//    /**
//     * 生成SM4密钥（128位）
//     *
//     * ⚠️ 安全警告：
//     * 1. 生成的密钥必须通过安全通道传输（如SM2加密）
//     * 2. 禁止将密钥硬编码或明文存储
//     * 3. 使用后建议调用clearByteArray()清零
//     *
//     * @return SecretKey SM4密钥对象
//     * @throws RuntimeException 密钥生成失败
//     */
//    public static SecretKey generateSm4Key() {
//        try {
//            KeyGenerator keyGen = KeyGenerator.getInstance("SM4", BC_PROVIDER);
//            keyGen.init(SM4_KEY_SIZE, new SecureRandom());
//            return keyGen.generateKey();
//        } catch (Exception e) {
//            log.error("SM4密钥生成失败", e);
//            throw new RuntimeException("SM4密钥生成失败", e);
//        }
//    }
//
//    /**
//     * SM4加密（CBC模式 + PKCS7Padding）
//     *
//     * 📌 模式说明：
//     * - CBC模式：需16字节IV（初始化向量），每次加密应使用随机IV
//     * - PKCS7Padding：兼容PKCS5，填充至16字节倍数
//     * - IV必须与密文一起传输（通常拼接在密文前）
//     *
//     * @param data 待加密数据
//     * @param key SM4密钥（SecretKey对象）
//     * @param iv 16字节初始化向量（建议使用generateIv()生成）
//     * @return 加密后的字节数组（建议：IV + 密文 拼接后传输）
//     * @throws RuntimeException 加密失败
//     */
//    public static byte[] sm4Encrypt(byte[] data, SecretKey key, byte[] iv) {
//        validateSm4Params(data, key, iv);
//        try {
//            Cipher cipher = Cipher.getInstance("SM4/CBC/PKCS7Padding", BC_PROVIDER);
//            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
//            return cipher.doFinal(data);
//        } catch (Exception e) {
//            log.error("SM4加密失败", e);
//            throw new RuntimeException("SM4加密失败", e);
//        }
//    }
//
//    /**
//     * SM4解密（CBC模式 + PKCS7Padding）
//     *
//     * @param ciphertext 密文（不含IV，IV需单独传入）
//     * @param key SM4密钥
//     * @param iv 16字节初始化向量（必须与加密时相同）
//     * @return 解密后的原始数据
//     * @throws RuntimeException 解密失败
//     */
//    public static byte[] sm4Decrypt(byte[] ciphertext, SecretKey key, byte[] iv) {
//        validateSm4Params(ciphertext, key, iv);
//        try {
//            Cipher cipher = Cipher.getInstance("SM4/CBC/PKCS7Padding", BC_PROVIDER);
//            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
//            return cipher.doFinal(ciphertext);
//        } catch (Exception e) {
//            log.error("SM4解密失败", e);
//            throw new RuntimeException("SM4解密失败", e);
//        }
//    }
//
//    /**
//     * 生成SM4加密所需的IV（16字节）
//     *
//     * 🔒 安全要求：
//     * - 必须使用SecureRandom生成
//     * - 每次加密使用唯一IV
//     * - IV无需保密，但需防篡改（建议与密文一起传输）
//     *
//     * @return 16字节IV
//     */
//    public static byte[] generateIv() {
//        byte[] iv = new byte[SM4_IV_SIZE];
//        new SecureRandom().nextBytes(iv);
//        return iv;
//    }
//
//    // ====================== 辅助方法（安全加固） ======================
//
//    /**
//     * 密文格式转换（C1C2C3 ↔ C1C3C2）
//     *
//     * 📌 格式说明：
//     * - C1C2C3: BouncyCastle默认输出格式（C1=65字节, C2=密文, C3=32字节）
//     * - C1C3C2: 国密标准GM/T 0003-2012规定格式（C1=65字节, C3=32字节, C2=密文）
//     * - 转换逻辑：提取C1(0-64), C3(65-96), C2(97-end) 重新拼接
//     *
//     * @param cipherText 原始密文
//     * @param toC1C3C2 true=转为C1C3C2, false=转为C1C2C3
//     * @return 转换后的密文
//     */
//    private static byte[] convertCipherText(byte[] cipherText, boolean toC1C3C2) {
//        // C1固定65字节（0x04开头的未压缩点）
//        int c1Len = 65;
//        // C3固定32字节（SM3摘要）
//        int c3Len = 32;
//
//        if (cipherText.length <= c1Len + c3Len) {
//            throw new IllegalArgumentException("密文长度异常，无法转换格式");
//        }
//
//        byte[] c1 = Arrays.copyOfRange(cipherText, 0, c1Len);
//        byte[] c3 = Arrays.copyOfRange(cipherText, c1Len, c1Len + c3Len);
//        byte[] c2 = Arrays.copyOfRange(cipherText, c1Len + c3Len, cipherText.length);
//
//        if (toC1C3C2) {
//            // C1C2C3 → C1C3C2
//            byte[] result = new byte[cipherText.length];
//            System.arraycopy(c1, 0, result, 0, c1Len);
//            System.arraycopy(c3, 0, result, c1Len, c3Len);
//            System.arraycopy(c2, 0, result, c1Len + c3Len, c2.length);
//            return result;
//        } else {
//            // C1C3C2 → C1C2C3
//            byte[] result = new byte[cipherText.length];
//            System.arraycopy(c1, 0, result, 0, c1Len);
//            System.arraycopy(c2, 0, result, c1Len, c2.length);
//            System.arraycopy(c3, 0, result, c1Len + c2.length, c3Len);
//            return result;
//        }
//    }
//
//    /**
//     * 安全清零字节数组（防止内存dump泄露敏感数据）
//     *
//     * 📌 使用场景：
//     * - 私钥字节数组使用后
//     * - 密文/明文处理完成后
//     * - 临时密钥变量销毁前
//     *
//     * @param bytes 需要清零的字节数组
//     */
//    public static void clearByteArray(byte[] bytes) {
//        if (bytes != null) {
//            Arrays.fill(bytes, (byte) 0);
//        }
//    }
//
//    /**
//     * 验证SM4参数合法性
//     */
//    private static void validateSm4Params(byte[] data, SecretKey key, byte[] iv) {
//        if (data == null || data.length == 0) {
//            throw new IllegalArgumentException("数据不能为空");
//        }
//        if (key == null || !"SM4".equals(key.getAlgorithm())) {
//            throw new IllegalArgumentException("无效的SM4密钥");
//        }
//        if (iv == null || iv.length != SM4_IV_SIZE) {
//            throw new IllegalArgumentException("IV必须为16字节");
//        }
//    }
//
//    // ====================== 实用工具方法 ======================
//
//    /**
//     * 将公钥转为16进制字符串（用于日志脱敏/传输）
//     *
//     * ⚠️ 注意：公钥可公开，但建议脱敏显示（如只显示前8位）
//     *
//     * @param publicKey 公钥
//     * @return 16进制字符串（无0x前缀）
//     */
//    public static String publicKeyToHex(PublicKey publicKey) {
//        return Hex.toHexString(publicKey.getEncoded());
//    }
//
//    /**
//     * 将私钥转为16进制字符串（⚠️ 仅限安全环境调试使用！）
//     *
//     * 🔒 严重警告：
//     * 1. 生产环境禁止调用此方法！
//     * 2. 私钥字符串必须立即清零
//     * 3. 建议仅用于KMS密钥导入等受控场景
//     *
//     * @param privateKey 私钥
//     * @return 16进制字符串
//     */
//    public static String privateKeyToHex(PrivateKey privateKey) {
//        log.warn("【安全警告】正在导出私钥！仅限受控环境使用");
//        return Hex.toHexString(privateKey.getEncoded());
//    }
//
//    /**
//     * 从16进制字符串恢复公钥
//     *
//     * @param hex 公钥16进制字符串
//     * @return PublicKey对象
//     * @throws RuntimeException 转换失败
//     */
//    public static PublicKey hexToPublicKey(String hex) {
//        try {
//            byte[] keyBytes = Hex.decode(hex);
//            KeyFactory keyFactory = KeyFactory.getInstance("EC", BC_PROVIDER);
//            return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
//        } catch (Exception e) {
//            log.error("公钥16进制字符串转换失败", e);
//            throw new RuntimeException("公钥转换失败", e);
//        }
//    }
//
//    /**
//     * 从16进制字符串恢复私钥（⚠️ 高危操作！）
//     *
//     * 🔒 安全要求：
//     * 1. 仅限KMS解密后临时使用
//     * 2. 使用后立即调用clearByteArray()清零
//     * 3. 禁止日志记录/网络传输
//     *
//     * @param hex 私钥16进制字符串
//     * @return PrivateKey对象
//     * @throws RuntimeException 转换失败
//     */
//    public static PrivateKey hexToPrivateKey(String hex) {
//        log.warn("【高危操作】正在导入私钥！确保在安全环境执行");
//        try {
//            byte[] keyBytes = Hex.decode(hex);
//            KeyFactory keyFactory = KeyFactory.getInstance("EC", BC_PROVIDER);
//            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
//        } catch (Exception e) {
//            log.error("私钥16进制字符串转换失败", e);
//            throw new RuntimeException("私钥转换失败", e);
//        }
//    }
//}
