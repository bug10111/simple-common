package com.simple.common.core.common.xss;

import cn.hutool.http.HtmlUtil;

/**
 * Created with IntelliJ IDEA
 * XSS 防护工具类
 * <p><b>安全边界说明（重要）</b>：{@link #clean(String)} 基于 Hutool {@link HtmlUtil#filter} 的黑名单正则清洗，
 * 属"粗过滤"，存在被非常规标签/属性编码、HTML实体编码 javascript: 等手法绕过的公开案例；且仅覆盖 HTML 上下文，
 * 不能作为属性、URL、JS、CSS 上下文的唯一防线。不要把本方法当作唯一防护——正确姿势是：
 * 存储前清洗 + 输出端按上下文转义（前端框架默认转义/富文本走白名单消毒器）。</p>
 * <p>{@link #escape(String)} 提供输出端 HTML 实体转义，用于把不可信文本安全嵌入 HTML 文档。</p>
 *
 * @author qty
 */
public final class XssUtils {

    private XssUtils() {
    }

    /**
     * 过滤HTML文本，防止XSS攻击
     * <p>契约：入参为 {@code null} 或空串时返回空串 {@code ""}（不回传null，降低调用方NPE风险）。</p>
     *
     * @param htmlContent HTML内容
     * @return 过滤后的内容，入参为null时返回""
     */
    public static String clean(String htmlContent) {
        if (htmlContent == null) {
            return "";
        }
        return HtmlUtil.filter(htmlContent);
    }

    /**
     * 输出端HTML实体转义（&lt; &gt; &amp; &quot; 等字符转为实体），
     * 用于把不可信文本安全地嵌入HTML文档/属性上下文
     *
     * @param text 原始文本
     * @return 实体转义后的文本，入参为null时返回""
     */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return HtmlUtil.escape(text);
    }
}
