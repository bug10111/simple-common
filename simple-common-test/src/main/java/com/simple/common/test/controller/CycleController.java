package com.simple.common.test.controller;

import com.simple.common.core.response.R;
import com.simple.common.test.common.entity.cycle.DataDemo;
import com.simple.common.test.service.DefaultCycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@RequestMapping("cycle")
@Tag(name = "循环任务调度")
@RestController
public class CycleController {

    @Autowired
    private DefaultCycleService defaultCycleService;

    @Operation(summary = "远程http查询订单")
    @PostMapping("select")
    public R<Object> select() {
        DataDemo dataDemo = new DataDemo().setDemoName2("测试数据1").setDemoName2("测试数据2");
        defaultCycleService.runAccumulate(dataDemo, 5, 1);
        return R.ok();
    }

}
