package com.simple.common.auth.client.event;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.event.SecretEvent;
import com.simple.common.auth.client.util.JJwtUtils;
import com.simple.common.auth.client.util.JwtUtils;
import com.simple.common.auth.client.util.SignSecretUtils;
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

    @EventListener
    public void onSecretChange(SecretEvent event) {
        String secret = event.getSecret();
        SecretEvent.SecretType secretType = event.getSecretType();
        
        // 如果没有指定密钥类型，记录警告并忽略
        if (secretType == null) {
            log.warn("接收到未指定密钥类型的SecretEvent，忽略处理");
            return;
        }
        
        switch (event.getOperation()) {
        case ADD, UPDATE -> {
            // 如果有clientId，说明是客户端级别的秘钥更新
            if (clientAuthInfo.getClient()) {
                switch (secretType) {
                    case JWT -> {
                        // 全局JWT秘钥更新
                        JJwtUtils.saveSecret(secret);
                        JwtUtils.saveSecret(secret);
                        log.info("全局JWT秘钥已更新。");
                    }
                    case SIGN -> {
                        // 签名密钥更新
                        SignSecretUtils.saveSecret(secret);
                        log.info("签名密钥已更新。");
                    }
                    default -> log.warn("未知的密钥类型: {}", secretType);
                }
            }
        }
        }
    }

}