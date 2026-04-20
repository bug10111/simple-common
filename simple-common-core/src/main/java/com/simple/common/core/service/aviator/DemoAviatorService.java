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
 *
 * @author qty
 */
@Component
public class DemoAviatorService extends DefAviatorFunction {

    @Override
    public String getName() {
        return "demo";
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3) {
        Number first = FunctionUtils.getNumberValue(arg1, env);
        Number second = FunctionUtils.getNumberValue(arg2, env);
        Number third = FunctionUtils.getNumberValue(arg3, env);
        return AviatorDecimal.valueOf(new BigDecimal(first.toString()).divide(new BigDecimal(second.toString()), Integer.parseInt(third.toString()), RoundingMode.HALF_UP));
    }
}
