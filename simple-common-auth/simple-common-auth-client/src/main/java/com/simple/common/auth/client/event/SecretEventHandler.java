package com.simple.common.auth.client.event;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.event.SecretEvent;
import com.simple.common.auth.client.common.manager.sign.SignManager;
import com.simple.common.auth.client.util.JJwtUtils;
import com.simple.common.auth.client.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * 秘钥事件处理器
 *
 * @author qty
 */
@Slf4j
@Component
public class SecretEventHandler {

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Autowired(required = false)
    private SignManager signManager;

    @EventListener
    public void onSecretChange(SecretEvent event) {
        String secret = event.getSecret();
        switch (event.getOperation()) {
        case ADD, UPDATE -> {
            // 如果有clientId，说明是客户端级别的秘钥更新
            if (clientAuthInfo.getClient()) {
                // 全局JWT秘钥更新
                JJwtUtils.saveSecret(secret);
                JwtUtils.saveSecret(secret);
                log.debug("全局JWT秘钥已更新。");
                
                // 签名密钥更新
                if (signManager != null) {
                    signManager.addSecret(secret);
                    log.debug("签名密钥已更新。");
                }
            }
        }
        }
    }

}