package com.simple.common.core.utils;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: SpEL表达式求值工具类
 *
 * @author qty
 */
public class SpringElUtils {

    /**
     * 解析并求值el表达式（无变量）
     *
     * @param expression 表达式
     * @return 表达式求值结果
     */
    public static Object getValue(String expression) {
        ExpressionParser parser = new SpelExpressionParser();
        Expression parsed = parser.parseExpression(expression);
        return parsed.getValue();
    }

    /**
     * 解析并求值el表达式，支持变量注入
     *
     * @param expression 表达式
     * @param variables  变量表，表达式内通过 #变量名 引用
     * @return 表达式求值结果
     */
    public static Object getValue(String expression, Map<String, Object> variables) {
        ExpressionParser parser = new SpelExpressionParser();
        Expression parsed = parser.parseExpression(expression);
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariables(variables);
        return parsed.getValue(context);
    }

}
