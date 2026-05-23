package com.simple.common.auth.client.event;

import com.simple.common.auth.client.common.event.SecretEvent;
import com.simple.common.auth.client.util.JJwtUtils;
import com.simple.common.auth.client.util.JwtUtils;
import com.simple.common.auth.client.util.SignSecretUtils;
import com.simple.common.core.common.properties.ApplicationProperties;
import com.simple.common.eventbus.common.annotation.EventHandler;
import com.simple.common.eventbus.common.constants.EventConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

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
    private ApplicationProperties applicationProperties;

    @EventHandler(applicationName = EventConstant.TARGET_ALL_X)
    public void onSecretChange(SecretEvent event) {
        String secret = event.getSecret();
        SecretEvent.SecretType secretType = event.getSecretType();
        String currentProjectCode = applicationProperties.getName();
        
        // 如果没有指定密钥类型，记录警告并忽略
        if (secretType == null) {
            log.warn("接收到未指定密钥类型的SecretEvent，忽略处理");
            return;
        }
        
        // 判断当前客户端是否应该处理该事件
        boolean shouldProcess;
        
        // 优先使用targetProjectCodes集合（多租户场景）
        List<String> targetProjectCodes = event.getTargetProjectCodes();
        if (targetProjectCodes != null && !targetProjectCodes.isEmpty()) {
            // 如果当前项目的client_name在目标集合中，则处理
            shouldProcess = targetProjectCodes.contains(currentProjectCode);
        } else {
            // 兼容旧逻辑：使用单个projectCode
            String eventProjectCode = event.getProjectCode();
            if (StringUtils.hasText(eventProjectCode)) {
                shouldProcess = eventProjectCode.equals(currentProjectCode);
            } else {
                // 如果都没有指定，默认处理（单项目场景）
                shouldProcess = true;
            }
        }
        
        if (!shouldProcess) {
            log.debug("接收到其他项目的密钥事件，当前项目 [{}]，忽略处理", currentProjectCode);
            return;
        }
        
        switch (event.getOperation()) {
        case ADD, UPDATE -> {
            switch (secretType) {
                case JWT -> {
                    JJwtUtils.saveSecret(secret);
                    JwtUtils.saveSecret(secret);
                    log.info("项目 [{}] 的JWT秘钥已更新。", currentProjectCode);
                }
                case SIGN -> {
                    SignSecretUtils.saveSecret(secret);
                    log.info("项目 [{}] 的签名密钥已更新。", currentProjectCode);
                }
                default -> log.warn("未知的密钥类型: {}", secretType);
            }
        }
        }
    }

}