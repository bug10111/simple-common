package com.simple.common.core.utils;

import cn.hutool.core.util.ClassUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: 类操作工具
 *
 * @author qty
 */
public class ClassUtils extends ClassUtil {

    private ClassUtils() {
    }

    /**
     * 通用的创建实例方法，通过传入的class类型创建并返回该类的实例
     *
     * @param clazz 要创建实例的class类型
     * @param <T>   要创建实例的类的类型
     * @return 创建的实例
     */
    public static <T> T createInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz不能为null");
        }
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            // 带类名定位：私有构造/抽象类/静态内部类等失败原因一目了然，不再@SneakyThrows裸抛
            throw new IllegalStateException("无法创建实例: " + clazz.getName(), e);
        }
    }

    /**
     * 通过属性名称在class中获取Field，并允许访问私有属性；沿继承链向上查找
     *
     * @param clazz     要查找的class类型
     * @param fieldName 属性名称
     * @return Field（不再静默返回null：找不到时抛出带类名/属性名的IllegalArgumentException，
     * 避免调用方漏判null在远处变成裸NPE）
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz不能为null");
        }
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
            throw new IllegalArgumentException("类" + clazz.getName() + "中不存在属性" + fieldName, e);
        }
    }

    /**
     * 通过属性名称在实例中获取数据
     *
     * @param t         实例
     * @param fieldName 属性名称
     * @return 属性值
     */
    public static <T> Object getFieldVar(T t, String fieldName) {
        if (t == null) {
            throw new IllegalArgumentException("实例不能为null");
        }
        // 显式校验替代assert：assert生产环境默认关闭（需-ea），缺失属性会在field.get处变成裸NPE
        Field field = getField(t.getClass(), fieldName);
        try {
            return field.get(t);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("读取属性失败: " + t.getClass().getName() + "." + fieldName, e);
        }
    }

    /**
     * 获取对象所有属性名称（沿继承链收集，跳过static与编译器合成字段）
     *
     * @param t 对象
     * @return 属性名称列表
     */
    public static <T> List<String> getFieldNames(T t) {
        if (t == null) {
            throw new IllegalArgumentException("对象不能为null");
        }
        List<String> list = new ArrayList<>();
        // 与getField同口径沿继承链收集，避免getFieldVar可读而getFieldNames漏列
        Class<?> clazz = t.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                    list.add(field.getName());
                }
            }
            clazz = clazz.getSuperclass();
        }
        return list;
    }
}
