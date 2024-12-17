package com.simple.common.core.utils;

import cn.hutool.core.codec.Base64;

/**
 * Created with IntelliJ IDEA
 * Description: Base64工具类
 *
 * @author 兄台丶请冷静
 */
public class Base64Utils {

    /**
     * base64编码
     *
     * @param text 编码前字符串
     */
    public static String encode(String text) {
        return Base64.encode(text);
    }

    /**
     * base64解码
     *
     * @param text 加密字符串
     */
    public static String decode(String text) {
        return Base64.decodeStr(text);
    }
}
