package com.simple.common.core.common.xss;

import com.simple.common.core.common.aspect.XssSafe;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA
 * <p>{@code @XssSafe} 的校验器：拒绝包含 HTML 标签/HTML 实体/HTML 敏感字符
 * （&lt;、&gt;、&quot;、&apos;、&amp;）的纯文本输入，属粗粒度预过滤——
 * 不覆盖 URL/属性上下文注入（如 {@code javascript:}、编码绕过），
 * 输出端仍需按上下文转义（见 {@link XssUtils#escape(String)}）。</p>
 *
 * @author qty
 */
public class XssValidator implements ConstraintValidator<XssSafe, String> {

    // 匹配HTML标签和常见XSS字符的正则表达式（字符黑名单项使标签/实体分支实际冗余，语义即"含任一HTML敏感字符即拒绝"）
    private static final Pattern XSS_PATTERN = Pattern.compile("<.*?>|&[a-zA-Z]+;|[<>\"'&]");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return !XSS_PATTERN.matcher(value).find();
    }
}
