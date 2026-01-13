package com.simple.common.core.utils;

/**
 * Created with IntelliJ IDEA
 * 进制转换工具
 *
 * @author qty
 */
public class DigitalTransformationUtils {

    /**
     * 十进制转化为十六进制
     */
    public static String bySixteen(Integer num) {
//        String s1 = String.format("%04X", Integer.parseInt(num + "")).toUpperCase();
        return Integer.toHexString((num & 0x000000FF) | 0xFFFFFF00).substring(6).toUpperCase();
    }

    ;

    /**
     * 十六进制转为十进制
     *
     * @param num
     * @return
     */
    public static String byTen(String num) {
        return Integer.valueOf(num, 16).toString();
    }

    ;

    public static void main(String[] args) {
//        String a3 = DigitalTransformationUtils.bySixteen(-93 + "");
//        System.out.println(a3);
//
//        String ffffffa3 = DigitalTransformationUtils.byTen("ffffffa3");
//        System.out.println(ffffffa3);
        byte[] bb = new byte[]{99, 11, 127};
        int byteVar = 9550;
//        String substring = Integer.toHexString((byteVar & 0x000000FF) | 0xFFFFFF00).substring(6).toUpperCase();
//        System.out.println(substring);
        System.out.println(Integer.toHexString(byteVar & 0xFF).toUpperCase());//FE
        System.out.println(Integer.toHexString(byteVar).toUpperCase());//FE
        System.out.println(Integer.toHexString(byteVar & 0x000000FF).toUpperCase());//FE
        System.out.println(DigitalTransformationUtils.bySixteen(byteVar));
//        String s = DigitalTransformationUtils.byTen("34372e39362e3134302e323433");
//        System.out.println(s);

        //十六进制 4占位输出
        String s1 = String.format("%04X", Integer.parseInt(byteVar + "")).toUpperCase();
        System.out.println(s1);
        System.out.println(String.format("%02d", 5));
    }

}
