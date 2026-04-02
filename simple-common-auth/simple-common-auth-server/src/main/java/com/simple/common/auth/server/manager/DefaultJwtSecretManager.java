package com.simple.common.auth.server.manager;

import com.simple.common.auth.server.common.event.SecretEvent;
import com.simple.common.auth.server.common.manager.secret.JwtSecretManager;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.eventbus.common.service.EventBusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA
 * Description: 默认秘钥管理器实现
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultJwtSecretManager implements JwtSecretManager {

    @Autowired
    private EventBusService eventBusService;

    /**
     * 当前有效的JWT秘钥
     */
    private volatile String currentSecret;

    /**
     * 初始化秘钥
     *
     * @param secret JWT秘钥
     */
    public void initSecret(String secret) {
        AssertUtils.notEmpty(secret, "秘钥不能为空");
        this.currentSecret = secret;
        
        // 发布秘钥初始化事件
        SecretEvent event = new SecretEvent();
        event.setSecret(secret);
        event.setOperation(SecretEvent.Operation.ADD);
        
        publishSecretEvent(event);
        
        log.info("JWT秘钥初始化成功");
    }

    @Override
    public void addSecret(String secret) {
        SecretEvent event = new SecretEvent();
        event.setSecret(secret);
        event.setOperation(SecretEvent.Operation.ADD);
        publishSecretEvent(event);
        currentSecret = secret;
        log.debug("JWT秘钥添加成功，当前秘钥已更新。");
    }

    @Override
    public void updateSecret(String oldSecret, String newSecret) {
        AssertUtils.notEmpty(newSecret, "新秘钥不能为空");
        AssertUtils.notEmpty(oldSecret, "旧秘钥不能为空");
        
        // 验证旧秘钥是否匹配
        AssertUtils.isTrue(oldSecret.equals(this.currentSecret), "旧秘钥不匹配");
        
        this.currentSecret = newSecret;
        
        // 发布秘钥更新事件
        SecretEvent event = new SecretEvent();
        event.setSecret(newSecret);
        event.setOperation(SecretEvent.Operation.UPDATE);
        
        publishSecretEvent(event);
        
        log.info("JWT秘钥更新成功");
    }

    @Override
    public String getCurrentSecret() {
        return this.currentSecret;
    }

    @Override
    public boolean existsSecret(String secret) {
        return secret != null && secret.equals(this.currentSecret);
    }

    /**
     * 发布秘钥变更事件
     *
     * @param event 秘钥事件
     */
    private void publishSecretEvent(SecretEvent event) {
        try {
            eventBusService.push(event);
            log.debug("秘钥变更事件发布成功，操作: {}", event.getOperation());
        } catch (Exception e) {
            log.error("秘钥变更事件发布失败", e);
            throw new RuntimeException("秘钥变更事件发布失败", e);
        }
    }

}