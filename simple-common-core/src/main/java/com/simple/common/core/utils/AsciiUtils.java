package com.simple.common.core.utils;

/**
 * Created with IntelliJ IDEA
 * Ascii转换
 *
 * @author qty
 */
public class AsciiUtils {

    private AsciiUtils() {
    }

    /**
     * 字符串转换为Ascii（每字符两位十六进制；入参null返回空串）
     */
    public static String stringTransformAscii(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sbu = new StringBuilder(value.length() * 2);
        for (char aChar : value.toCharArray()) {
            // bySixteen仅支持0~255：非单字节字符（如中文）此处直接拒绝，避免静默截断
            sbu.append(DigitalTransformationUtils.bySixteen((int) aChar));
        }
        return sbu.toString();
    }

    /**
     * Ascii转换为字符串（O(n)解析；奇数长度或非法十六进制字符快速失败）
     */
    public static String asciiTransformString(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        // 奇数长度的十六进制串无法两两成组，直接拒绝而非静默丢弃末位
        if (value.length() % 2 != 0) {
            throw new IllegalArgumentException("十六进制串长度必须为偶数: " + value);
        }
        StringBuilder sbu = new StringBuilder(value.length() / 2);
        for (int i = 0; i < value.length(); i += 2) {
            String segment = value.substring(i, i + 2);
            sbu.append((char) Integer.parseInt(DigitalTransformationUtils.byTen(segment)));
        }
        return sbu.toString();
    }
}
