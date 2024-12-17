package com.simple.common.core.utils;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: aviator计算引擎帮助类
 *
 * @author 兄台丶请冷静
 */
public class AviatorUtils {

    /**
     * 执行表达式
     *
     * @param expression 表达式
     * @param vars       参数和数据(表达式变量，对应变量值)
     * @param r          返回的数据类型
     * @return 返回的数据
     */
    public static <R> R run(String expression, Map<String, Object> vars, Class<R> clazz) {
        Expression compile = AviatorEvaluator.compile(expression);
        Object result = compile.execute(vars);
        return clazz.cast(result);
    }
}
