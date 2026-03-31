package com.simple.common.auth.client.common.event;

import com.simple.common.auth.client.util.JJwtUtils;
import com.simple.common.auth.client.util.JwtUtils;
import com.simple.common.eventbus.common.annotation.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 秘钥变更事件处理器
 *
 * @author qty
 */
@Slf4j
@Component
public class SecretEventHandler {

    @EventHandler
    public void onSecretChange(SecretEvent event) {
        String newSecret = event.getNewSecret();
        String oldSecret = event.getOldSecret();
        String eventType = event.getEventType();

        log.info("收到秘钥变更事件，类型: {}", eventType);

        // 更新JWT工具类的秘钥
        JwtUtils.saveSecret(newSecret);
        JJwtUtils.saveSecret(newSecret);

        log.info("秘钥更新成功，旧秘钥: {}, 新秘钥: {}", 
                oldSecret != null ? oldSecret.substring(0, Math.min(8, oldSecret.length())) + "***" : "无", 
                newSecret.substring(0, Math.min(8, newSecret.length())) + "***");
    }
}
