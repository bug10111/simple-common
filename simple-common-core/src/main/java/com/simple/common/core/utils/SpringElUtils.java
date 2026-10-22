package com.simple.common.core.utils;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: SpEL表达式求值工具类
 * <p><b>安全契约（重要）</b>：本工具基于 {@link StandardEvaluationContext}（支持 {@code T(...)}/构造器/Bean引用等完整能力），
 * 仅限<b>可信来源</b>的表达式（业务配置、代码内常量）——含用户输入的表达式存在SpEL注入（任意代码执行）风险，
 * 严禁直接传入不可信输入；不可信输入场景请自行改用 {@code SimpleEvaluationContext} 并按本类变量注入方式构建只读上下文。</p>
 *
 * @author qty
 */
public class SpringElUtils {

    /**
     * 线程安全的共享解析器（ExpressionParser无状态，复用减少热路径重复分配与解析开销）
     */
    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private SpringElUtils() {
    }

    /**
     * 解析并求值el表达式（无变量）
     *
     * @param expression 表达式（不能为空，须为可信来源，见类安全契约）
     * @return 表达式求值结果
     */
    public static Object getValue(String expression) {
        return getValue(expression, null);
    }

    /**
     * 解析并求值el表达式，支持变量注入
     *
     * @param expression 表达式（不能为空，须为可信来源，见类安全契约）
     * @param variables  变量表，表达式内通过 #变量名 引用；null按无变量处理（StandardEvaluationContext#setVariables对null会NPE，这里显式守卫）
     * @return 表达式求值结果
     */
    public static Object getValue(String expression, Map<String, Object> variables) {
        Expression parsed = PARSER.parseExpression(expression);
        StandardEvaluationContext context = new StandardEvaluationContext();
        if (variables != null) {
            context.setVariables(variables);
        }
        return parsed.getValue(context);
    }
}
