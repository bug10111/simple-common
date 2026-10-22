package com.simple.common.core.utils;

import cn.hutool.core.util.StrUtil;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Expression;
import com.googlecode.aviator.Options;
import com.googlecode.aviator.Feature;

import java.util.Collections;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: aviator计算引擎帮助类
 * <p>安全说明：表达式可能来自业务配置/用户输入，这里使用<b>沙箱化</b>独立求值器——
 * 禁用 {@code T(...) 反射调用}(NewInstance)、module 与内部变量等高危特性，并限制单次求值超时与循环次数，
 * 杜绝 {@code T(java.lang.Runtime)...}、无限循环类RCE/DoS。原生全局求值器请勿直接用于不可信表达式。</p>
 *
 * @author qty
 */
public final class AviatorUtils {

    /**
     * 单次表达式求值超时（毫秒）
     */
    private static final long EVAL_TIMEOUT_MS = 1000L;

    /**
     * 单次表达式最大循环次数
     */
    private static final int MAX_LOOP_COUNT = 10_000;

    /**
     * 沙箱求值器：独立实例，不与全局共享函数注册表（自定义函数经AviatorInit注册到全局，本工具保持纯净沙箱）
     */
    private static final AviatorEvaluatorInstance SANDBOX_EVALUATOR = AviatorEvaluator.getInstance();

    static {
        // 仅保留无副作用的常用语言特性；排除 NewInstance(T(...)反射调用)、Module、内部变量等高危面
        SANDBOX_EVALUATOR.setOption(Options.FEATURE_SET, Feature.asSet(
                Feature.Assignment, Feature.Return, Feature.If,
                Feature.Let, Feature.Lambda,
                Feature.StringInterpolation, Feature.ExceptionHandle));
        SANDBOX_EVALUATOR.setOption(Options.EVAL_TIMEOUT_MS, EVAL_TIMEOUT_MS);
        SANDBOX_EVALUATOR.setOption(Options.MAX_LOOP_COUNT, MAX_LOOP_COUNT);
    }

    private AviatorUtils() {
    }

    /**
     * 执行表达式（沙箱模式，禁用反射调用等高危特性）
     *
     * @param expression 表达式（不能为空）
     * @param vars       参数和数据(表达式变量，对应变量值)，null按空env处理
     * @return 计算结果
     */
    public static Object run(String expression, Map<String, Object> vars) {
        if (StrUtil.isBlank(expression)) {
            throw new IllegalArgumentException("expression 不能为空");
        }
        Map<String, Object> env = vars == null ? Collections.emptyMap() : vars;
        try {
            // cached=false：表达式可能含请求态输入，无界缓存会成为内存DoS点
            Expression compiled = SANDBOX_EVALUATOR.compile(expression, false);
            return compiled.execute(env);
        } catch (Exception e) {
            // 包装失败上下文，避免裸异常丢失表达式定位信息（超时/语法/特性受限在此区分）
            throw new IllegalStateException("表达式执行失败: " + expression, e);
        }
    }
}
