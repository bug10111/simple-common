package com.simple.common.core.utils;

import cn.hutool.core.bean.BeanUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 对象操作工具
 *
 * @author qty
 */
@Slf4j
public class BeanUtils extends BeanUtil {

    /**
     * 将对象转化为map
     *
     * @param obj 对象
     */
    public static Map<String, Object> toMap(Object obj) {
        return beanToMap(obj, false, true);
    }

    /**
     * 将map的key和对象属性相同的值，赋予对象
     *
     * @param map    源数据
     * @param tClass 需要赋予的对象class
     * @param <T>    赋值后的对象
     */
    @SneakyThrows
    public static <T> T fillBeanWithMap(Map<?, ?> map, Class<T> tClass) {
        T t = tClass.getDeclaredConstructor().newInstance();
        return fillBeanWithMap(map, t, true);
    }

    // 新增缓存（线程安全）
    //    private static final ConcurrentHashMap<String, BeanCopier> BEAN_COPIER_CACHE = new ConcurrentHashMap<>();

    //    /**
    //     * 基于 CGLIB，性能较高,但是和@Accessors(chain = true)冲突，无法复制属性
    //     *
    //     * @param source 源数据
    //     * @param tClass 目标对象
    //     */
    //    @SneakyThrows
    //    public static <T> T copyProperties(Object source, Class<T> tClass) {
    //        if (source == null) {
    //            return null;
    //        }
    //
    //        // 获取或创建 BeanCopier（线程安全）
    //        Class<?> sourceClass = source.getClass();
    //        String cacheKey = sourceClass.getName() + "->" + tClass.getName();
    //        BeanCopier copier = BEAN_COPIER_CACHE.computeIfAbsent(cacheKey, key -> BeanCopier.create(sourceClass, tClass, true));
    //
    //        // 实例化并拷贝
    //        T target = tClass.getDeclaredConstructor().newInstance();
    //        copier.copy(source, target, new IgnoreReturnTypeConverter());
    //        return target;
    //    }

    //    /**
    //     * 自定义 Converter：忽略 Setter 的返回类型
    //     * 解决了@Accessors(chain = true)冲突，但是同名称不同属性类型会报错
    //     */
    //    private static class IgnoreReturnTypeConverter implements Converter {
    //        @Override
    //        public Object convert(Object sourceValue, Class targetType, Object context) {
    //            // 直接返回原值，BeanCopier 会尝试调用参数类型匹配的 Setter（无论返回类型是否 void）
    //            return sourceValue;
    //        }
    //    }
}
