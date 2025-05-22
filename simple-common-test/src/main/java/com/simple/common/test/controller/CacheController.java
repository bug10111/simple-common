package com.simple.common.test.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.simple.common.cache.common.factory.LocalCacheFactory;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.test.common.entity.doc.DocTestEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@RequestMapping("cache")
@Tag(name = "缓存")
@RestController
public class CacheController {

    @Autowired
    private LocalCacheFactory<String,String> cacheFactory;

    @Autowired
    private LocalCacheFactory<String, DocTestEntity> objFactory;

    @Operation(summary = "字符串缓存")
    @GetMapping("string")
    @SneakyThrows
    public R<Object> string() {
        Cache<String, String> string = cacheFactory.createCache("string", config -> config.initialCapacity(100).expireAfterWrite(5));
        string.put("key", "value");
        log.debug(string.getIfPresent("key"));
        log.debug(string.get("key",s -> "重新读取value"));

        Thread.sleep(7000);
        log.debug(string.getIfPresent("key"));
        log.debug(string.get("key",s -> "重新读取value"));

        return R.ok();
    }

    @Operation(summary = "对象缓存")
    @GetMapping("obj")
    @SneakyThrows
    public R<Object> obj() {
        Cache<String, DocTestEntity> string = objFactory.createCache("string", config -> config.initialCapacity(100).expireAfterWrite(5));
        string.put("key", new DocTestEntity().setName("名字").setAge(18).setSex("女"));
        log.debug(JsonUtils.toJsonStr(string.getIfPresent("key")));
        log.debug(JsonUtils.toJsonStr(string.get("key",s -> new DocTestEntity().setName("名字1").setAge(18).setSex("女") )));

        Thread.sleep(7000);
        log.debug(JsonUtils.toJsonStr(string.getIfPresent("key")));
        log.debug(JsonUtils.toJsonStr(string.get("key",s -> new DocTestEntity().setName("名字1").setAge(18).setSex("女") )));

        return R.ok();
    }
}
