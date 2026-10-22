package com.simple.common.core.utils;

import cn.hutool.core.util.StrUtil;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA
 * Description: 路由帮助类
 * 表达式规则如下：
 * ? 表示单个字符
 * * 匹配多个字符
 * ** 匹配多层路径
 * 符号可以匹配中间层（root/符号/list）
 * <p>匹配安全说明：入参路径统一做规范化（去 {@code ;} 路径参数、折叠连续斜杠、消解 {@code ./} 与 {@code ../} 段），
 * 防止 {@code /api/admin;jsessionid=x} 之类的原始URI绕过 {@code /api/admin/**} 保护规则。</p>
 *
 * @author qty
 */
public class UrlRulesUtils {

    //spring 自带的，和hutool二选一，用法都一样
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

    static {

        //去两端空格
        antPathMatcher.setTrimTokens(true);

        //缓存
        antPathMatcher.setCachePatterns(true);

        //区分大小写
        antPathMatcher.setCaseSensitive(true);
    }

    private UrlRulesUtils() {
    }

    /**
     * 判断指定url地址是否匹配指定url集合中的任意一个
     *
     * @param urlPath 指定url地址（支持携带路径参数/不规范斜杠，内部先规范化）
     * @param urls    需要检查的url集合（规则集合，null/空元素跳过）
     * @return 是否匹配  匹配返回true，不匹配返回false
     */
    public static boolean matches(String urlPath, Collection<String> urls) {
        if (StrUtil.isEmpty(urlPath) || urls == null || urls.isEmpty()) {
            return false;
        }
        String normalizedPath = normalizePath(urlPath);
        for (String url : urls) {
            // 跳过null/空规则（如配置空行），避免AntPathMatcher对null规则NPE打断整个匹配
            if (StrUtil.isNotEmpty(url) && isMatch(url, normalizedPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断指定url地址是否匹配指定url集合中的任意一个
     *
     * @param urlPath 指定url地址（内部先规范化）
     * @param urls    需要检查的url集合（规则集合，null/空元素跳过）
     * @return 是否匹配  匹配返回true，不匹配返回false
     */
    public static boolean matches(String urlPath, String[] urls) {
        if (StrUtil.isEmpty(urlPath) || urls == null) {
            return false;
        }
        String normalizedPath = normalizePath(urlPath);
        for (String url : urls) {
            if (StrUtil.isNotEmpty(url) && isMatch(url, normalizedPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断url是否与规则配置匹配
     *
     * @param pattern 匹配规则
     * @param urlPath 需要匹配的url（注意：本方法不做规范化，集合入口已统一处理）
     */
    public static boolean isMatch(String pattern, String urlPath) {
        return antPathMatcher.match(pattern, urlPath);
    }

    /**
     * 判断两个集合是否存在交集
     *
     * @param set1 集合1（null按空处理返回false）
     * @param set2 集合2（null按空处理返回false）
     */
    public static boolean hasIntersection(Set<?> set1, Set<?> set2) {
        if (set1 == null || set2 == null || set1.isEmpty() || set2.isEmpty()) return false;
        // 小集合做外层遍历，降低均摊复杂度
        Set<?> outer = set1.size() <= set2.size() ? set1 : set2;
        Set<?> inner = outer == set1 ? set2 : set1;
        for (Object item : outer) {
            if (inner.contains(item)) return true;
        }
        return false;
    }

    /**
     * 查找路径命中的规则
     * <p>确定性保证：多条规则同时命中时返回AntPathMatcher语义下<b>最具体</b>的一条
     * （如 {@code /api/admin/**} 优先于 {@code /api/**}），不受入参Set迭代顺序影响——
     * 该返回值被授权链用作角色查找key，必须保证跨实例一致。</p>
     *
     * @param path     请求路径（支持携带路径参数/不规范斜杠，内部先规范化）
     * @param patterns 规则集合（null/空元素跳过）
     * @return 命中的最具体规则；无命中或入参非法时返回null
     */
    public static String findMatch(String path, Set<String> patterns) {
        if (StrUtil.isEmpty(path) || patterns == null || patterns.isEmpty()) {
            // 与matches保持一致的空值语义：无入参即无命中，不向授权链抛NPE
            return null;
        }
        String normalizedPath = normalizePath(path);
        List<String> hitPatterns = new ArrayList<>();
        for (String pattern : patterns) {
            if (StrUtil.isNotEmpty(pattern) && isMatch(pattern, normalizedPath)) {
                hitPatterns.add(pattern);
            }
        }
        if (hitPatterns.isEmpty()) {
            return null;
        }
        // 多规则命中时按Spring的pattern比较器取最具体者，消除Set迭代顺序带来的授权不确定性
        hitPatterns.sort(antPathMatcher.getPatternComparator(normalizedPath));
        return hitPatterns.get(0);
    }

    /**
     * 判断表达式是否合法
     *
     * @param path 表达式
     */
    public static boolean isPattern(String path) {
        return antPathMatcher.isPattern(path);
    }

    /**
     * 路径规范化：去 {@code ;} 路径参数（如 jsessionid）、折叠连续斜杠、消解 {@code ./} 与 {@code ../} 段
     */
    private static String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        // 1. 截断路径参数（每个;段到下一个/为止）
        int semi = path.indexOf(';');
        if (semi >= 0) {
            StringBuilder builder = new StringBuilder(path.length());
            int index = 0;
            while (index < path.length()) {
                int nextSemi = path.indexOf(';', index);
                if (nextSemi < 0) {
                    builder.append(path, index, path.length());
                    break;
                }
                builder.append(path, index, nextSemi);
                int slash = path.indexOf('/', nextSemi);
                index = slash < 0 ? path.length() : slash;
            }
            path = builder.toString();
        }
        // 2. 折叠连续斜杠
        path = path.replaceAll("/{2,}", "/");
        // 3. 消解 ./ 与 ../ 段
        String[] segments = path.split("/");
        List<String> resolved = new ArrayList<>(segments.length);
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                // 越过根的 .. 丢弃（不出根）
                if (!resolved.isEmpty()) {
                    resolved.remove(resolved.size() - 1);
                }
                continue;
            }
            resolved.add(segment);
        }
        return "/" + String.join("/", resolved);
    }
}
