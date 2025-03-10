package com.simple.common.core.common.xss;

import com.simple.common.core.common.aspect.XssSafe;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
public class XssValidator implements ConstraintValidator<XssSafe, String> {

    // 匹配HTML标签和常见XSS字符的正则表达式
    private static final Pattern XSS_PATTERN = Pattern.compile("<.*?>|&[a-zA-Z]+;|[<>\"'&]");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return !XSS_PATTERN.matcher(value).find();
    }
}
