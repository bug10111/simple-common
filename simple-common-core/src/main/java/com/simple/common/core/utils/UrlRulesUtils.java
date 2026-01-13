package com.simple.common.core.utils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA
 * 路由帮助类
 * 表达式规则如下：
 * ? 表示单个字符
 * * 匹配多个字符
 * ** 匹配多层路径
 * 符号可以匹配中间层（root/符号/list）
 *
 * @author qty
 */
@Slf4j
public class UrlRulesUtils {

    //    private static final AntPathMatcher antPathMatcher = new AntPathMatcher().setCaseSensitive(true).setTrimTokens(true);

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

    /**
     * 判断指定url地址是否匹配指定url集合中的任意一个
     *
     * @param urlPath 指定url地址
     * @param urls    需要检查的url集合（规则集合）
     * @return 是否匹配  匹配返回true，不匹配返回false
     */
    public static boolean matches(String urlPath, Collection<String> urls) {
        if (StrUtil.isEmpty(urlPath) || CollectionUtils.isEmpty(urls)) {
            return false;
        }
        for (String url : urls) {
            if (isMatch(url, urlPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断指定url地址是否匹配指定url集合中的任意一个
     *
     * @param urlPath 指定url地址
     * @param urls    需要检查的url集合 （规则集合）
     * @return 是否匹配  匹配返回true，不匹配返回false
     */
    public static boolean matches(String urlPath, String[] urls) {
        if (StrUtil.isEmpty(urlPath) || urls == null) {
            return false;
        }
        for (String url : urls) {
            if (isMatch(url, urlPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断url是否与规则配置:
     *
     * @param url     匹配规则
     * @param urlPath 需要匹配的url
     */
    public static boolean isMatch(String url, String urlPath) {
        return antPathMatcher.match(url, urlPath);
    }

    /**
     * 判断表达式是否合法
     *
     * @param path 表达式
     */
    public static boolean isPattern(String path) {
        return antPathMatcher.isPattern(path);
    }
}
