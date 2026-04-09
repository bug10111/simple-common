package com.simple.common.auth.server.manager;

import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.manager.cache.CacheManager;
import com.simple.common.auth.server.common.manager.client.ClientManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 默认客户端管理器
 *
 * @author Admin
 */
@Component
public class DefaultClientManager implements ClientManager {

    @Autowired
    private CacheManager cacheManager;

    @Override
    public void saveClient(String clientId, ClientDetails clientDetails) {
        String key = CLIENT_KEY_PREFIX + clientId;
        cacheManager.set(key, clientDetails.getClientSecret(), clientDetails.getExpire());
    }

    @Override
    public String getClientSecret(String clientId) {
        String key = CLIENT_KEY_PREFIX + clientId;
        return cacheManager.get(key);
    }

    @Override
    public void deleteClient(String clientId) {
        String key = CLIENT_KEY_PREFIX + clientId;
        cacheManager.delete(key);
    }
}