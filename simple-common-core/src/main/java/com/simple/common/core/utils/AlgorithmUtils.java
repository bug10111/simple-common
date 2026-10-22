package com.simple.common.core.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.asymmetric.KeyType;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 算法工具类，包括MD5、SHA-512
 * 注意，严禁使用MD5加密密码等敏感数据，MD5不再安全；md5Hex系列仅限非安全场景的校验和用途（如分片去重、对象键派生）
 * 摘要算法是一种能产生特殊输出格式的算法，这种算法的特点是：无论用户输入什么长度的原始数据，经过计算后输出的密文都是固定长度的
 *
 * @author qty
 */
@Deprecated
public class AlgorithmUtils {

    /**
     * 生成Bcrypt加密后的密文
     *
     * @param password 明文密码
     * @return 加密后的密文
     * @since 4.1.1
     */
    public static String bcrypt(String password) {
        return DigestUtil.bcrypt(password);
    }

    /**
     * 验证密码是否与Bcrypt加密后的密文匹配
     * <p>契约：入参为空或哈希串损坏（非法salt版本等）时返回false而不是抛异常，
     * 保证认证流程中损坏的存量凭证按"不匹配"处理而非中断。</p>
     *
     * @param password 明文密码
     * @param hashed   hash值（加密后的值）
     * @return 是否匹配
     * @since 4.1.1
     */
    public static boolean bcryptCheck(String password, String hashed) {
        if (StrUtil.isBlank(password) || StrUtil.isBlank(hashed)) {
            return false;
        }
        try {
            return DigestUtil.bcryptCheck(password, hashed);
        } catch (IllegalArgumentException e) {
            // 哈希串损坏/版本不支持：按不匹配处理，避免认证流程被底层异常打断
            return false;
        }
    }

    /**
     * 计算SHA-512摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return SHA-512摘要的16进制表示
     */
    public static String sha512Hex(final String data) {
        return DigestUtil.sha512Hex(data);
    }

    /**
     * 计算SHA-512摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return SHA-512摘要的16进制表示
     */
    public static String sha512Hex(final InputStream data) {
        return DigestUtil.sha512Hex(data);
    }

    /**
     * 计算SHA-256摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return SHA-512摘要的16进制表示
     */
    public static String sha512Hex(final byte[] data) {
        return DigestUtil.sha512Hex(data);
    }

    /**
     * 计算SHA-512摘要值，并转为16进制字符串
     *
     * @param data    被摘要数据
     * @param charset 编码
     * @return SHA-512摘要的16进制表示
     */
    public static String sha512Hex(final String data, final String charset) {
        return DigestUtil.sha512Hex(data, charset);
    }

    /**
     * 计算SHA-512摘要值，并转为16进制字符串
     *
     * @param file 被摘要文件
     * @return SHA-512摘要的16进制表示
     */
    public static String sha512Hex(final File file) {
        return DigestUtil.sha512Hex(file);
    }

    /**
     * 计算SHA-512摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return SHA-512摘要的16进制表示
     */
    public static String sha256Hex(final String data) {
        return DigestUtil.sha256Hex(data);
    }

    /**
     * 计算SHA-512摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return SHA-512摘要的16进制表示
     */
    public static String sha256Hex(final InputStream data) {
        return DigestUtil.sha256Hex(data);
    }

    /**
     * 计算SHA-256摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return SHA-512摘要的16进制表示
     */
    public static String sha256Hex(final byte[] data) {
        return DigestUtil.sha256Hex(data);
    }

    /**
     * 计算SHA-512摘要值，并转为16进制字符串
     *
     * @param data    被摘要数据
     * @param charset 编码
     * @return SHA-512摘要的16进制表示
     */
    public static String sha256Hex(final String data, final String charset) {
        return DigestUtil.sha256Hex(data, charset);
    }

    /**
     * 计算SHA-512摘要值，并转为16进制字符串
     *
     * @param file 被摘要文件
     * @return SHA-512摘要的16进制表示
     */
    public static String sha256Hex(final File file) {
        return DigestUtil.sha256Hex(file);
    }

    /**
     * 计算32位MD5摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return MD5摘要的16进制表示
     */
    public static String md5Hex(String data) {
        return DigestUtil.md5Hex(data);
    }

    /**
     * 计算32位MD5摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return MD5摘要的16进制表示
     */
    public static String md5Hex(InputStream data) {
        return DigestUtil.md5Hex(data);
    }

    /**
     * 计算32位MD5摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return MD5摘要的16进制表示
     */
    public static String md5Hex(byte[] data) {
        return DigestUtil.md5Hex(data);
    }

    /**
     * 计算32位MD5摘要值，并转为16进制字符串
     *
     * @param data    被摘要数据
     * @param charset 编码
     * @return MD5摘要的16进制表示
     */
    public static String md5Hex(String data, String charset) {
        return DigestUtil.md5Hex(data, charset);
    }

    /**
     * 计算32位MD5摘要值，并转为16进制字符串
     *
     * @param data    被摘要数据
     * @param charset 编码
     * @return MD5摘要的16进制表示
     * @since 4.6.0
     */
    public static String md5Hex(String data, Charset charset) {
        return DigestUtil.md5Hex(data, charset);
    }

    /**
     * 计算32位MD5摘要值，并转为16进制字符串
     *
     * @param file 被摘要文件
     * @return MD5摘要的16进制表示
     */
    public static String md5Hex(File file) {
        return DigestUtil.md5Hex(file);
    }

    /**
     * 计算SHA-512摘要值，并转为16进制字符串
     *
     * @param data    被摘要数据
     * @param charset 编码
     * @return SHA-512摘要的16进制表示
     */
    public static String sha512Hex(final String data, final Charset charset) {
        // 委托Hutool空安全Charset重载，与兄弟重载行为一致，不再因null charset抛NPE
        return DigestUtil.sha512Hex(data, charset == null ? null : charset.name());
    }

    /**
     * 获取新的RSA秘钥对（显式2048位，不再依赖Hutool默认1024位）
     *
     * @return RSA秘钥对，包含公钥和私钥
     */
    public static Map<String, String> createRsaStr() {
        java.security.KeyPair kp = cn.hutool.crypto.KeyUtil.generateKeyPair("RSA", 2048);
        RSA rsa = new RSA(kp.getPrivate(), kp.getPublic());
        Map<String, String> keyPair = new HashMap<>();
        keyPair.put("privateKey", rsa.getPrivateKeyBase64());
        keyPair.put("publicKey", rsa.getPublicKeyBase64());
        return keyPair;
    }

}