package com.simple.common.auth.client.event;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.event.SecretEvent;
import com.simple.common.auth.client.util.JJwtUtils;
import com.simple.common.auth.client.util.JwtUtils;
import com.simple.common.auth.client.util.SignSecretUtils;
import com.simple.common.core.common.properties.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

    @Autowired
    private ApplicationProperties applicationProperties;

    @EventListener
    public void onSecretChange(SecretEvent event) {
        String secret = event.getSecret();
        SecretEvent.SecretType secretType = event.getSecretType();
        String eventProjectCode = event.getProjectCode();
        String currentProjectCode = applicationProperties.getName();
        
        // 如果没有指定密钥类型，记录警告并忽略
        if (secretType == null) {
            log.warn("接收到未指定密钥类型的SecretEvent，忽略处理");
            return;
        }
        
        // 如果事件中指定了projectCode，且与当前项目不匹配，则忽略
        if (StringUtils.hasText(eventProjectCode) && !eventProjectCode.equals(currentProjectCode)) {
            log.debug("接收到其他项目 [{}] 的密钥事件，当前项目 [{}]，忽略处理", eventProjectCode, currentProjectCode);
            return;
        }
        
        switch (event.getOperation()) {
        case ADD, UPDATE -> {
            // 如果是客户端模式，加载密钥
            if (clientAuthInfo.getClient()) {
                switch (secretType) {
                    case JWT -> {
                        // 全局JWT秘钥更新
                        JJwtUtils.saveSecret(secret);
                        JwtUtils.saveSecret(secret);
                        log.info("项目 [{}] 的JWT秘钥已更新。", currentProjectCode);
                    }
                    case SIGN -> {
                        // 签名密钥更新
                        SignSecretUtils.saveSecret(secret);
                        log.info("项目 [{}] 的签名密钥已更新。", currentProjectCode);
                    }
                    default -> log.warn("未知的密钥类型: {}", secretType);
                }
            }
        }
        }
    }

}