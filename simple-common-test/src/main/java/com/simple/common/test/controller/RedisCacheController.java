package com.simple.common.test.controller;

import com.simple.common.core.response.R;
import com.simple.common.redis.annotation.RedisCache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@RequestMapping("redis")
@Tag(name = "分布式缓存")
@RestController
public class RedisCacheController {

    @Operation(summary = "分布式缓存")
    @PostMapping("cache")
    @RedisCache
    @SneakyThrows
    public R<List<String>> list() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add("测试数据：" + i);
            Thread.sleep(10);
        }
        return R.ok(list);
    }
}
