package com.simple.common.test.controller;

import com.simple.common.core.response.R;
import com.simple.common.core.utils.AviatorUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA on 2023/11/2 16:32
 *
 * @author qty
 */
@Slf4j
@RequestMapping("aviator")
@Tag(name = "计算引擎")
@RestController
public class AviatorController {

    @Operation(summary = "aviator计算")
    @PostMapping("aviator")
    public R<Object> aviator(String exp, BigDecimal var1, BigDecimal var2, BigDecimal var3) {
        exp = "demo(var1, var2, var3) * sum";
        Map<String, Object> map = new HashMap<>();
        map.put("var1", var1);
        map.put("var2", var2);
        map.put("var3", var3);
        map.put("sum", 10);
        Object run = AviatorUtils.run(exp, map);
        return R.ok(run);
    }

}
