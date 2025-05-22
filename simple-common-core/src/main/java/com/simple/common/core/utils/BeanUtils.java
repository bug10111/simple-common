package com.simple.common.core.utils;

import cn.hutool.core.bean.BeanUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.beans.BeanCopier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA
 * Description: 对象操作工具
 *
 * @author 兄台丶请冷静
 */
@Slf4j
public class BeanUtils extends BeanUtil {

    // 新增缓存（线程安全）
    private static final ConcurrentHashMap<String, BeanCopier> BEAN_COPIER_CACHE = new ConcurrentHashMap<>();

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

    /**
     * 基于 CGLIB，性能较高
     *
     * @param source 源数据
     * @param tClass 目标对象
     */
    @SneakyThrows
    public static <T> T copyProperties(Object source, Class<T> tClass) {
        if (source == null) {
            return null;
        }

        // 获取或创建 BeanCopier（线程安全）
        Class<?> sourceClass = source.getClass();
        String cacheKey = sourceClass.getName() + "->" + tClass.getName();
        BeanCopier copier = BEAN_COPIER_CACHE.computeIfAbsent(cacheKey, key -> BeanCopier.create(sourceClass, tClass, false));

        // 实例化并拷贝
        T target = tClass.getDeclaredConstructor().newInstance();
        copier.copy(source, target, null);
        return target;
    }
}
