package com.simple.common.test.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.simple.common.cache.Utils.CacheUtils;
import com.simple.common.cache.common.factory.LocalCacheFactory;
import com.simple.common.cache.common.function.GetDBFunction;
import com.simple.common.cache.common.function.GetRedisFunction;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.test.common.entity.doc.DocTestEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@RequestMapping("cache")
@Tag(name = "缓存")
@RestController
public class CacheController implements InitializingBean {

    @Autowired
    private LocalCacheFactory<String, String> cacheFactory;

    @Autowired
    private LocalCacheFactory<String, DocTestEntity> objFactory;

    private Cache<String, String> string;

    @Operation(summary = "字符串缓存")
    @GetMapping("string")
    @SneakyThrows
    public R<Object> string() {
        log.debug(string.get("key", s -> "重新读取value"));
        log.debug(string.getIfPresent("key"));
        return R.ok();
    }

    @Operation(summary = "获取字符串缓存")
    @GetMapping("getString")
    @SneakyThrows
    public R<Object> getString() {
        log.debug(string.getIfPresent("key"));
        return R.ok();
    }

    @Operation(summary = "对象缓存")
    @GetMapping("obj")
    @SneakyThrows
    public R<Object> obj() {
        Cache<String, DocTestEntity> string = objFactory.createCache("string", config -> config.initialCapacity(100).expireAfterWrite(5));
        string.put("key", new DocTestEntity().setName("名字").setAge(18).setSex("女"));
        log.debug(JsonUtils.toJsonStr(string.getIfPresent("key")));
        log.debug(JsonUtils.toJsonStr(string.get("key", s -> new DocTestEntity().setName("名字1").setAge(18).setSex("女"))));

        Thread.sleep(7000);
        log.debug(JsonUtils.toJsonStr(string.getIfPresent("key")));
        log.debug(JsonUtils.toJsonStr(string.get("key", s -> new DocTestEntity().setName("名字1").setAge(18).setSex("女"))));

        return R.ok();
    }

    @Operation(summary = "redis")
    @GetMapping("redis/string")
    @SneakyThrows
    public R<Object> redisObj() {

        DocTestEntity docTestEntity = new DocTestEntity().setName("名字").setAge(18).setSex("女");

        GetRedisFunction<DocTestEntity, DocTestEntity> getRedisFunction = new GetRedisFunction<>() {
            @Override
            public DocTestEntity get(DocTestEntity request) {
                request.setName("名字redis");
                return null;
            }

            @Override
            public void setHandler(DocTestEntity value) {
                log.debug("赋值：==>{}", JsonUtils.toJsonStr(value));
            }

            @Override
            public DocTestEntity getNullable() {
                return new DocTestEntity();
            }
        };
        GetDBFunction<DocTestEntity, DocTestEntity> getDBFunction = (request) -> {
            request.setName("名字DB");
            log.debug("从数据库加载新的数据==>{}", JsonUtils.toJsonStr(request));
            return request;
        };

        DocTestEntity docTestEntity1 = CacheUtils.get(docTestEntity, "localKey", getRedisFunction, getDBFunction);
        log.debug(JsonUtils.toJsonStr(docTestEntity1));

        return R.ok();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
         string = cacheFactory.createCache("string", config -> config.maximumSize(100));

    }
}
