package com.simple.common.core.utils;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
public class SpringElUtils {

    /**
     * 解析el
     *
     * @param expression 表达式
     */
    public static Object getValue(String expression) {
        ExpressionParser parser = new SpelExpressionParser();
        return parser.parseExpression(expression);
    }

}
