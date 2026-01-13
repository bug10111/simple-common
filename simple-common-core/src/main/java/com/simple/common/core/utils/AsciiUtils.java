package com.simple.common.core.utils;

/**
 * Created with IntelliJ IDEA
 * Ascii转换
 *
 * @author qty
 */
public class AsciiUtils {

    /**
     * 字符串转换为Ascii
     */
    public static String stringTransformAscii(String value) {
        StringBuilder sbu = new StringBuilder();
        char[] chars = value.toCharArray();
        for (char aChar : chars) {
            sbu.append(DigitalTransformationUtils.bySixteen((int) aChar));
        }
        return sbu.toString();
    }

    /**
     * Ascii转换为字符串
     */
    public static String asciiTransformString(String value) {
        StringBuilder val = new StringBuilder(value);
        StringBuilder sbu = new StringBuilder();
        while (val.length() >= 2) {
            String substring = val.substring(0, 2);
            val.delete(0, 2);
            sbu.append((char) Integer.parseInt(DigitalTransformationUtils.byTen(substring)));
        }
        return sbu.toString();
    }

    public static void main(String[] args) {
        //字符串转换为Ascii的案例
        String stringTransformAscii = stringTransformAscii("47.96.140.243");
        System.out.println("字符串转换为Ascii:" + stringTransformAscii);

        //Ascii转换为字符串的案例
        String asciiTransformString = asciiTransformString("333832363933377933642E35317669702E62697A ");
        System.out.println("Ascii转换为字符串:" + asciiTransformString);
    }
}
