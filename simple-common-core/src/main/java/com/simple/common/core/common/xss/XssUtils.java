package com.simple.common.core.common.xss;

import cn.hutool.http.HtmlUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Created with IntelliJ IDEA
 * XSS 防护工具类
 *
 * @author 兄台丶请冷静
 */
@Slf4j
public class XssUtils {

    /**
     * 过滤HTML文本，防止XSS攻击
     *
     * @param htmlContent HTML内容
     * @return 过滤后的内容
     */
    public static String clean(String htmlContent) {
        return HtmlUtil.filter(htmlContent);
    }
}
