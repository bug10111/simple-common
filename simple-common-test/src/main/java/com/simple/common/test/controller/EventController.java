package com.simple.common.test.controller;

import com.simple.common.core.response.R;
import com.simple.common.test.common.event.EventTestRequest;
import com.simple.common.test.common.service.EvenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA on 2023/11/2 16:32
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@RequestMapping("event")
@Tag(name = "事件")
@RestController
public class EventController {

    @Autowired
    private EvenService evenService;

    @Operation(summary = "触发事件")
    @PostMapping("handler")
    public R<Object> handler(@RequestBody @Validated EventTestRequest request) {
        evenService.testSend(request);
        return R.ok();
    }

    @Operation(summary = "触发延迟事件")
    @PostMapping("delayHandler")
    public R<Object> delayHandler(@RequestBody @Validated EventTestRequest request) {
        evenService.testSend(request, 5, TimeUnit.SECONDS);
        return R.ok();
    }
}
