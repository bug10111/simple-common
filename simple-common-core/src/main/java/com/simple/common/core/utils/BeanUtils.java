package com.simple.common.core.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 对象操作工具
 *
 * @author qty
 */
public class BeanUtils extends BeanUtil {

    private BeanUtils() {
    }

    /**
     * 将对象转化为map
     * <p>契约（注意）：底层为 Hutool {@code beanToMap(bean, false, true)}——
     * key保持属性原名；值为null的属性<b>不会</b>出现在map里（签名/回写场景需注意null属性会丢失）；
     * 入参为null时返回null（与"无属性对象转出空map"明确区分）。</p>
     *
     * @param obj 对象
     * @return 属性map
     */
    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return null;
        }
        return beanToMap(obj, false, true);
    }

    /**
     * 将map中key与对象属性相同的值，赋予对象
     *
     * @param map    源数据（不能为null）
     * @param tClass 需要赋值的对象class（不能为null）
     * @param <T>    赋值后的对象
     * @return 赋值后的对象
     */
    public static <T> T fillBeanWithMap(Map<?, ?> map, Class<T> tClass) {
        if (map == null) {
            throw new IllegalArgumentException("map不能为null");
        }
        if (tClass == null) {
            throw new IllegalArgumentException("tClass不能为null");
        }
        try {
            T t = tClass.getDeclaredConstructor().newInstance();
            // 显式CopyOptions：忽略类型转换错误保持既有行为（与原true等价），但策略自文档化
            return fillBeanWithMap(map, t, CopyOptions.create().setIgnoreError(true));
        } catch (ReflectiveOperationException e) {
            // 无默认构造器/私有构造器/抽象类在此处带类名快速失败，不再@SneakyThrows裸抛NoSuchMethodException
            throw new IllegalStateException("无法实例化目标类型: " + tClass.getName(), e);
        }
    }
}
