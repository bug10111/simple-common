package com.simple.common.oauth.start.event;

import com.simple.common.core.common.properties.ApplicationProperties;
import com.simple.common.core.utils.CryptoUtil;
import com.simple.common.eventbus.common.annotation.EventHandler;
import com.simple.common.eventbus.common.constants.EventConstant;
import com.simple.common.oauth.start.common.event.sysClientDetails.SysClientDetailsCreatedEvent;
import com.simple.common.oauth.start.common.event.sysClientDetails.SysClientDetailsRemovedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.KeyPair;

/**
 * Created with IntelliJ IDEA
 * Description: 客户端事件实现
 *
 * @author qty
 */
@Slf4j
@Component
public class SysClientDetailsEvent {

    @Autowired
    private ApplicationProperties applicationProperties;

    // RSA密钥缓存
    private final java.util.Map<String, KeyPair> rsaKeyCache = new java.util.concurrent.ConcurrentHashMap<>();

    @EventHandler(applicationName = EventConstant.TARGET_ALL_X)
    public void create(SysClientDetailsCreatedEvent event) {
        if(applicationProperties.getName().equals(event.getServer())){
            // 从PEM格式恢复密钥对
            KeyPair keyPair = CryptoUtil.restoreKeyPair(event.getRsaPublic(), event.getRsaPrivate());
            rsaKeyCache.put(event.getClientId(), keyPair);
            log.info("客户端[{}]RSA秘钥同步完毕", event.getClientId());
        }
    }

    @EventHandler(applicationName = EventConstant.TARGET_ALL_X)
    public void delete(SysClientDetailsRemovedEvent event) {
        if(applicationProperties.getName().equals(event.getServer())){
            rsaKeyCache.remove(event.getClientId());
            log.info("客户端[{}]RSA秘钥删除完毕", event.getClientId());
        }
    }
}
