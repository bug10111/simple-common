package com.simple.common.core.utils;

import cn.hutool.core.collection.CollectionUtil;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA
 * Description: 集合精确匹配帮助类（注意：纯元素精确相等判断，无模式/路由语义；
 * 路由模式匹配请使用 {@link UrlRulesUtils}）
 *
 * @author qty
 */
public class CollectionUtils {

    private CollectionUtils() {
    }

    /**
     * 判断指定元素是否在集合中存在（精确相等匹配，不做模式匹配）
     *
     * @param target 待查找元素
     * @param list   需要检查的集合（null按不存在处理，恒返回false）
     * @return 是否存在
     */
    public static boolean matches(String target, Collection<String> list) {
        if (target == null || list == null || list.isEmpty()) {
            return false;
        }
        return CollectionUtil.contains(list, target);
    }

    /**
     * 判断两个集合是否存在交集（containsAny对称操作，参数顺序不影响结果）
     *
     * @param specify 集合1（null按空处理）
     * @param list    集合2（null按空处理）
     * @return 是否存在交集
     */
    public static boolean matches(Collection<String> specify, Collection<String> list) {
        if (specify == null || specify.isEmpty() || list == null || list.isEmpty()) {
            return false;
        }
        return CollectionUtil.containsAny(list, specify);
    }
}
