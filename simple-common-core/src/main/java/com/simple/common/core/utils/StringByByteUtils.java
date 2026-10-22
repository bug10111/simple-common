package com.simple.common.core.utils;

/**
 * Created with IntelliJ IDEA
 * 字符串和字节转换
 *
 * @author qty
 */
public class StringByByteUtils {

    private StringByByteUtils() {
    }

    /**
     * 将16进制的字节数组，转化为内容为16进制的字符串
     *
     * @param src 待转换的字节数组
     * @return 十六进制字符串，入参为null或空数组时返回空串""（不再回传null，降低调用方NPE风险）
     */
    public static String bytesToHexString(byte[] src) {
        if (src == null || src.length <= 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder(src.length * 2);
        for (byte b : src) {
            int v = b & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    /**
     * 将十六进制字符串 转化为字节
     *
     * @param hexString 十六进制字符串，长度必须为偶数且仅含0-9/A-F/a-f字符
     * @return 字节数组，入参为null或空字符串时返回null
     * @throws IllegalArgumentException 当字符串长度为奇数或包含非法十六进制字符时抛出
     */
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.isEmpty()) {
            return null;
        }
        hexString = hexString.toUpperCase();
        // 奇数长度的十六进制串无法按两字符一组切分，直接拒绝而非静默丢弃末位
        if (hexString.length() % 2 != 0) {
            throw new IllegalArgumentException("十六进制字符串长度必须为偶数，实际长度: " + hexString.length() + "，输入: " + hexString);
        }
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            // 非法字符在字符表中查得-1，会参与移位产生错位字节，必须拒绝
            int high = charToByte(hexChars[pos]);
            int low = charToByte(hexChars[pos + 1]);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("输入包含非法十六进制字符，输入: " + hexString);
            }
            d[i] = (byte) (high << 4 | low);
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    /**
     * 将指定byte数组以16进制的形式打印到控制台（兼容保留，新代码请使用 {@link #toHexString(byte[])}）
     *
     * @param b 待打印的字节数组（null或空数组直接返回，不再NPE）
     */
    public static void printHexString(byte[] b) {
        System.out.print(toHexString(b));
    }

    /**
     * 将byte数组转为16进制字符串（返回字符串而非直接打印，便于调用方控制输出目标）
     *
     * @param b 待转换的字节数组
     * @return 大写16进制字符串，入参为null或空数组时返回空串""
     */
    public static String toHexString(byte[] b) {
        if (b == null || b.length == 0) {
            return "";
        }
        StringBuilder hex = new StringBuilder(b.length * 2);
        for (byte value : b) {
            String h = Integer.toHexString(value & 0xFF);
            if (h.length() == 1) {
                hex.append('0');
            }
            hex.append(h);
        }
        return hex.toString().toUpperCase();
    }

}
