package com.simple.common.core.utils;

/**
 * Created with IntelliJ IDEA
 * 进制转换工具
 * <p>契约：{@link #bySixteen(Integer)} 仅接受 0~255（单字符/字节语义，超出部分按低8位截断）；
 * {@link #byTen(String)} 按32位无符号解析（8位十六进制高位为1也能安全转换）。</p>
 *
 * @author qty
 */
public class DigitalTransformationUtils {

    private DigitalTransformationUtils() {
    }

    /**
     * 十进制转化为十六进制（两位大写；入参取低8位，仅0~255可无损还原）
     */
    public static String bySixteen(Integer num) {
        if (num == null) {
            throw new IllegalArgumentException("bySixteen入参不能为null");
        }
        if (num < 0 || num > 0xFF) {
            throw new IllegalArgumentException("bySixteen仅支持0~255（单字符语义）: " + num);
        }
        return Integer.toHexString(num).toUpperCase();
    }

    /**
     * 十六进制转为十进制（按无符号32位解析，"ffffffa3"等高位为1的串不再NumberFormatException）
     */
    public static String byTen(String num) {
        if (num == null || num.isBlank()) {
            throw new IllegalArgumentException("byTen入参不能为空");
        }
        try {
            return Integer.toUnsignedString(Integer.parseUnsignedInt(num, 16));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("byTen入参不是合法的32位十六进制串: " + num, e);
        }
    }
}
