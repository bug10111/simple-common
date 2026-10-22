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
public final class Base64Utils {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private Base64Utils() {
    }

    /**
     * 将字符串进行base64编码
     *
     * @param text 编码前字符串
     * @return base64字符串，入参为null时返回null
     */
    public static String encode(String text) {
        if (text == null) {
            return null;
        }
        return encode(text.getBytes(DEFAULT_CHARSET));
    }

    /**
     * 将字节进行base64编码
     *
     * @param text 编码前字节
     * @return base64字符串，入参为null时返回null
     */
    public static String encode(byte[] text) {
        if (text == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(text);
    }

    /**
     * base64解码为字符串
     * <p>仅用于文本载荷：二进制内容（图片/密文等）经UTF-8解码会静默替换非法序列丢数据，
     * 二进制请使用 {@link #decode(String)} 自行按原始字节处理。</p>
     *
     * @param text 加密字符串
     * @return 解码后的字符串，入参为null时返回null
     */
    public static String decodeStr(String text) {
        byte[] bytes = decode(text);
        return bytes == null ? null : new String(bytes, DEFAULT_CHARSET);
    }

    /**
     * base64解码为字节数组
     * <p>非法base64输入抛 {@link IllegalArgumentException}（JDK行为，保持透传并在调用方显式处理）。</p>
     *
     * @param text 加密字符串
     * @return 字节数组，入参为null时返回null
     */
    public static byte[] decode(String text) {
        if (text == null) {
            return null;
        }
        return Base64.getDecoder().decode(text);
    }
}
