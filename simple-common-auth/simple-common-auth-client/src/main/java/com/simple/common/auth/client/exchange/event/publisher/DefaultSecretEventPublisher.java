package com.simple.common.auth.client.exchange.event.publisher;

import com.simple.common.auth.client.exchange.event.event.SecretEvent;
import com.simple.common.auth.client.exchange.event.publisher.SecretEventPublisher;
import com.simple.common.eventbus.common.service.EventBusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 默认密钥事件发布器实现。
 * <p>
 * 通过 EventBus 将密钥变更事件发布到 Fanout 交换器 "event.all"，
 * 广播到所有绑定的客户端项目。
 * </p>
 *
 * <h3>职责：</h3>
 * <ul>
 *   <li>仅负责广播，不涉及本地缓存</li>
 *   <li>仅在服务端模式下使用</li>
 * </ul>
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultSecretEventPublisher implements SecretEventPublisher {

    @Autowired
    private EventBusService eventBusService;

    @Override
    public void broadcastJwtSecret(String secret, List<String> targetProjectCodes) {
        SecretEvent event = new SecretEvent();
        event.setTargetProjectCodes(targetProjectCodes);
        event.setSecret(secret);
        event.setOperation(SecretEvent.Operation.ADD);
        event.setSecretType(SecretEvent.SecretType.JWT);
        eventBusService.push(event);

        log.info("JWT密钥已广播，目标项目: {}", targetProjectCodes);
    }

    @Override
    public void broadcastSignSecret(String secret, List<String> targetProjectCodes) {
        SecretEvent event = new SecretEvent();
        event.setTargetProjectCodes(targetProjectCodes);
        event.setSecret(secret);
        event.setOperation(SecretEvent.Operation.ADD);
        event.setSecretType(SecretEvent.SecretType.SIGN);
        eventBusService.push(event);

        log.info("签名密钥已广播，目标项目: {}", targetProjectCodes);
    }
}
