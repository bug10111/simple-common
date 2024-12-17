package com.simple.common.core.utils;

import cn.hutool.core.util.ClassUtil;
import lombok.SneakyThrows;

import java.lang.reflect.Field;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
public class ClassUtils extends ClassUtil {

    /**
     * 通用的创建实例方法，通过传入的class类型创建并返回该类的实例
     *
     * @param clazz 要创建实例的class类型
     * @param <T>   要创建实例的类的类型
     * @return 创建的实例
     */
    @SneakyThrows
    public static <T> T createInstance(Class<T> clazz) {
        return clazz.getDeclaredConstructor().newInstance();
    }

    /**
     * 通过属性名称在class中获取Field，并允许获取私有属性
     *
     * @param clazz     要创建实例的class类型
     * @param fieldName 属性名称
     */
    @SneakyThrows
    public static Field getField(Class<?> clazz, String fieldName) {
        try {
            Field declaredField = clazz.getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            return declaredField;
        } catch (NoSuchFieldException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                // 递归查找父类
                return getField(superClass, fieldName);
            }
        }
        return null;
    }

    /**
     * 通过属性名称在实例中获取数据
     *
     * @param t         实例
     * @param fieldName 要创建实例的class类型
     */
    @SneakyThrows
    public static <T> Object getFieldVar(T t, String fieldName) {
        Field field = getField(t.getClass(), fieldName);
        assert field != null : "不存在" + fieldName + "属性";
        return field.get(t);
    }
}
