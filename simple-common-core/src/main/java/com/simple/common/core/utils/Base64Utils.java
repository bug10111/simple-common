package com.simple.common.core.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Created with IntelliJ IDEA
 * Description: Base64工具类
 *
 * @author qty
 */
public class Base64Utils {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * 将字符串进行base64编码
     *
     * @param text 编码前字符串
     */
    public static String encode(String text) {
        return encode(text.getBytes(DEFAULT_CHARSET));
    }

    /**
     * 将字节进行base64编码
     *
     * @param text 编码前字节
     */
    public static String encode(byte[] text) {
        return Base64.getEncoder().encodeToString(text);
    }

    /**
     * base64解码为字符串
     *
     * @param text 加密字符串
     */
    public static String decodeStr(String text) {
        return new String(decode(text), DEFAULT_CHARSET);
    }

    /**
     * base64解码为字节数组
     *
     * @param text 加密字符串
     */
    public static byte[] decode(String text) {
        return Base64.getDecoder().decode(text);
    }
}
