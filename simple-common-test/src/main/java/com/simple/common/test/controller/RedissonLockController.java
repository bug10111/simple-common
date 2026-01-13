package com.simple.common.test.controller;

import com.simple.common.core.function.DefaultFunction;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.redis.common.service.RedissonLockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * Created by IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@RequestMapping("lock")
@Tag(name = "分布式锁")
@RestController
public class RedissonLockController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonLockService lockService;

    @Operation(summary = "初始化库存")
    @PostMapping("/add")
    public R<Object> addStock() {
        stringRedisTemplate.opsForValue().set("stock", 50 + "");
        stringRedisTemplate.opsForValue().set("park", 50 + "");
        return R.ok();
    }

    @Operation(summary = "可重入锁-扣库存")
    @PostMapping("/send1")
    public R<Object> sendSms1(String key) {

        DefaultFunction function = () -> {
            String str = stringRedisTemplate.opsForValue().get("stock");
            AssertUtils.notEmpty(str, "库存不存在");
            int stock = Integer.parseInt(str);
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock", realStock + "");
                log.info("扣减库存成功！，剩余库存：{}", realStock);
            } else {
                log.info("扣减失败！库存不足！");
            }
        };
        lockService.lock(key, function);

        return R.ok();
    }

    @Operation(summary = "闭锁-上锁")
    @GetMapping("/lockdoor")
    public R<Object> lockdoor() {
        lockService.countDownLatch("123", 2);
        return R.ok();
    }

    @Operation(summary = "闭锁-解锁条件触发")
    @GetMapping("/leave")
    public R<Object> leave() {
        lockService.decreaseCountDownLatch("123");
        return R.ok();
    }

    @Operation(summary = "信号量")
    @GetMapping("/semaphoreLock")
    @ResponseBody
    public R<Object> semaphoreLock() {
        lockService.semaphoreLock("123", 10);
        return R.ok();
    }

    @Operation(summary = "信号量-增加")
    @GetMapping("/increaseSemaphoreLock")
    @ResponseBody
    public R<Object> increaseSemaphoreLock() {
        lockService.increaseSemaphoreLock("123", 1);
        return R.ok();
    }

    @Operation(summary = "信号量-扣除")
    @GetMapping("/decreaseSemaphoreLock")
    @ResponseBody
    public R<Object> decreaseSemaphoreLock() {
        lockService.decreaseSemaphoreLock("123", 1);
        return R.ok();
    }
}
