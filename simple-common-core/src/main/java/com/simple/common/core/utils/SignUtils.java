package com.simple.common.core.utils;

import lombok.SneakyThrows;

import java.lang.reflect.Field;

/**
 * Created with IntelliJ IDEA
 * Description: 签名工具类
 *
 * @author 兄台丶请冷静
 */
public class SignUtils {

    /**
     * 获取当前对象所有属性名和class，并生成字符串拼接
     *
     * @param t 目标对象
     */
    public static <T> String getSignStr(T t) {
        StringBuilder stringBuilder = new StringBuilder();
        append(t.getClass(), stringBuilder, t);
        return stringBuilder.toString();
    }

    /**
     * 拼接字符串
     *
     * @param aClass        class
     * @param stringBuilder 字符串
     */
    @SneakyThrows
    private static <T> void append(Class<?> aClass, StringBuilder stringBuilder, T t) {

        // 使用反射获取 base 的所有属性
        Field[] fields = aClass.getDeclaredFields();

        for (Field field : fields) {

            // 允许访问私有属性
            field.setAccessible(true);

            // 获取属性值
            Object value = field.get(t);
            if (value != null) {
                if (!stringBuilder.isEmpty()) {
                    stringBuilder.append("&");
                }
                stringBuilder.append(field.getName()).append("=").append(value);
            }
        }

        if (aClass.getSuperclass() != null) {
            append(aClass.getSuperclass(), stringBuilder, t);
        }
    }

}
