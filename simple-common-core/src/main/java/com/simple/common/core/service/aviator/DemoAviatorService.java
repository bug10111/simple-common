package com.simple.common.core.service.aviator;

import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorDecimal;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.simple.common.core.common.service.aviator.DefAviatorFunction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 自定义function demo
 * <p>demo(a, b, c)：计算 a / b，结果保留 c 位小数（HALF_UP）。
 * 入参未解析/不可数值、除数为0、精度越界都会以带参数名定位的表达式异常快速失败，
 * 不再以裸NPE/ArithmeticException打断表达式求值链路。</p>
 *
 * @author qty
 */
@Component
public class DemoAviatorService extends DefAviatorFunction {

    /**
     * 保留位数上限（防止恶意超大精度触发高成本计算）
     */
    private static final int MAX_SCALE = 100;

    @Override
    public String getName() {
        return "demo";
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3) {
        Number first = FunctionUtils.getNumberValue(arg1, env);
        Number second = FunctionUtils.getNumberValue(arg2, env);
        Number third = FunctionUtils.getNumberValue(arg3, env);

        // 入参未解析(nil)或不可数值时getNumberValue返回null，先校验并给出定位信息
        if (first == null || second == null || third == null) {
            throw new ArithmeticException("demo函数入参必须是可解析数值: first=" + first + ", second=" + second + ", scale=" + third);
        }

        BigDecimal dividend = toBigDecimal(first, "first");
        BigDecimal divisor = toBigDecimal(second, "second");

        // 除数为0时给出明确表达式错误，替代底层ArithmeticException: Division by zero
        if (divisor.signum() == 0) {
            throw new ArithmeticException("demo函数除数(second)不能为0");
        }

        int scale = parseScale(third);

        return AviatorDecimal.valueOf(dividend.divide(divisor, scale, RoundingMode.HALF_UP));
    }

    private static BigDecimal toBigDecimal(Number value, String argName) {
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            throw new ArithmeticException("demo函数入参" + argName + "不是合法数值: " + value);
        }
    }

    private static int parseScale(Number third) {
        int scale;
        try {
            scale = Integer.parseInt(third.toString());
        } catch (NumberFormatException e) {
            throw new ArithmeticException("demo函数精度入参(scale)必须是整数: " + third);
        }
        if (scale < 0 || scale > MAX_SCALE) {
            throw new ArithmeticException("demo函数精度入参(scale)必须在0~" + MAX_SCALE + "之间: " + scale);
        }
        return scale;
    }
}
