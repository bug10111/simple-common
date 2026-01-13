package com.simple.common.core.utils;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import com.simple.common.core.response.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: aviator计算引擎帮助类
 *
 * @author qty
 */
public class AviatorUtils {

    /**
     * 执行表达式
     *
     * @param expression 表达式
     * @param vars       参数和数据(表达式变量，对应变量值)
     * @return 返回的数据
     */
    public static Object run(String expression, Map<String, Object> vars) {
        Expression compile = AviatorEvaluator.compile(expression);
        return compile.execute(vars);
    }

    public static void main(String[] args) {
        String exp = "(var1 + var2 - var3) * sum";
        Map<String, Object> map = new HashMap<>();
        map.put("var1", 1);
        map.put("var2", 2);
        map.put("var3", 2);
        map.put("sum", 10);
        Object run =  AviatorUtils.run(exp, map);
        for (String str : map.keySet()) {
            System.out.println("参数：" + str + " = " + map.get(str));
        }
        System.out.println("计算公示：" + exp + "    计算结果：" + run);
    }
}
