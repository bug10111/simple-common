package com.simple.common.core.utils;

import cn.hutool.core.bean.BeanUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 对象操作工具
 *
 * @author 兄台丶请冷静
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
}
