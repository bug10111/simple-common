package com.simple.common.auth.client.manager.auth;

import com.simple.common.auth.client.common.manager.auth.WhiteManager;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Created with IntelliJ IDEA
 * <p>
 * 默认白名单管理器实现
 *
 * @author qty
 */
@Component
public class DefaultWhiteManager implements WhiteManager {

    private static final String WHITE_LIST_KEY = "auth:white:list";
    
    private final CacheManager cacheManager;

    public DefaultWhiteManager(CacheManager authCacheManager) {
        this.cacheManager = authCacheManager;
    }

    @Override
    public void addWhiteList(Set<String> urls) {
        for (String url : urls) {
            cacheManager.set(WHITE_LIST_KEY + ":" + url, "1");
        }
    }

    @Override
    public boolean isWhite(String url) {
        return cacheManager.hasKey(WHITE_LIST_KEY + ":" + url);
    }
}