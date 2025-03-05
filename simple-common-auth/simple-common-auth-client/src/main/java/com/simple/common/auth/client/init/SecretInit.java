package com.simple.common.auth.client.init;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.client.util.JJwtUtils;
import com.simple.common.auth.client.util.JwtUtils;
import com.simple.common.core.common.enums.order.SimpleOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 初始化jwt
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Component
public class SecretInit implements ApplicationListener<ApplicationReadyEvent>, Ordered {

    @Autowired
    private AuthProperties authProperties;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        JwtUtils.saveSecret(authProperties.getJWTSigner());
        JJwtUtils.saveSecret(authProperties.getJWTSigner());
    }

    @Override
    public int getOrder() {
        return SimpleOrder.Oauth.getOrder();
    }
}
