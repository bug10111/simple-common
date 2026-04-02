package com.simple.common.auth.client.common.event;

import com.simple.common.auth.client.util.JJwtUtils;
import com.simple.common.auth.client.util.JwtUtils;
import com.simple.common.core.utils.RsaUtils;
import jakarta.annotation.Resource;
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

    @EventListener
    public void onSecretChange(SecretEvent event) {
        String clientId = event.getClientId();
        String secret = event.getSecret();
        switch (event.getOperation()) {
            case ADD, UPDATE -> {
                // 如果有clientId，说明是客户端级别的秘钥更新
                if (clientId != null && !clientId.isEmpty()) {
                    log.debug("客户端[{}]秘钥已更新。", clientId);
                } else {
                    // 全局JWT秘钥更新
                    JJwtUtils.saveSecret(secret);
                    JwtUtils.saveSecret(secret);
                    log.debug("全局JWT秘钥已更新。");
                }
            }
            case DELETE -> log.warn("秘钥删除事件暂未处理。");
        }
    }

}