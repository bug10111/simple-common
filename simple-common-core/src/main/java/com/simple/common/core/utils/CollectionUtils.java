package com.simple.common.core.utils;

import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA
 * 路由帮助类，这是直接匹配的帮助类
 *
 * @author qty
 */
@Slf4j
public class CollectionUtils {

    /**
     * 判断指定元素是否在Collection中存在
     *
     * @param specify 指定
     * @param list    需要检查的url集合（规则集合）
     */
    public static boolean matches(String specify, Collection<String> list) {
        return CollectionUtil.contains(list, specify);
    }

    /**
     * 两个Collection是否存在交集
     *
     * @param specify specify
     * @param list    list
     */
    public static boolean matches(Collection<String> specify, Collection<String> list) {
        return CollectionUtil.containsAny(list, specify);
    }
}
