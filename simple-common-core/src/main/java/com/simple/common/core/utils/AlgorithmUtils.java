package com.simple.common.core.utils;

import cn.hutool.crypto.digest.DigestUtil;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Created with IntelliJ IDEA
 * Description: 算法工具类，包括MD5、SHA-512
 * 注意，严禁使用MD5加密密码登敏感数据，MD5不再安全
 * 摘要算法是一种能产生特殊输出格式的算法，这种算法的特点是：无论用户输入什么长度的原始数据，经过计算后输出的密文都是固定长度的
 *
 * @author 兄台丶请冷静
 */
public class AlgorithmUtils {

    /**
     * 生成Bcrypt加密后的密文
     *
     * @param password 明文密码
     * @return 加密后的密文
     * @since 4.1.1
     */
    public static String bcrypt(String password) {
        //        return BCrypt.hashpw(password, BCrypt.gensalt());
        return DigestUtil.bcrypt(password);
    }

    /**
     * 验证密码是否与Bcrypt加密后的密文匹配
     *
     * @param password 明文密码
     * @param hashed   hash值（加密后的值）
     * @return 是否匹配
     * @since 4.1.1
     */
    public static boolean bcryptCheck(String password, String hashed) {
        //        return BCrypt.checkpw(password, hashed);
        return DigestUtil.bcryptCheck(password, hashed);
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
     * 计算SHA-1摘要值，并转为16进制字符串
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

}
