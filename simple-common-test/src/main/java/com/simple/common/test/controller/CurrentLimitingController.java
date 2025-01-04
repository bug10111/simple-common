package com.simple.common.test.controller;

import com.simple.common.core.response.R;
import com.simple.common.redis.annotation.CurrentLimiting;
import com.simple.common.redis.annotation.RedisCache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@RequestMapping("limiting")
@Tag(name = "分布式限流")
@RestController
public class CurrentLimitingController {

    @Operation(summary = "限流")
    @GetMapping
    @CurrentLimiting(time = 5, waitingTime = 2)
    public R<Object> CurrentLimiting() {
        return R.ok();
    }
}
