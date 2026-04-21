package com.simple.common.oauth.start.init;

import com.googlecode.aviator.AviatorEvaluator;
import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.core.common.enums.order.SimpleOrder;
import com.simple.common.core.common.properties.ApplicationProperties;
import com.simple.common.core.common.service.aviator.DefAviatorFunction;
import com.simple.common.core.utils.CryptoUtil;
import com.simple.common.oauth.start.common.dto.ApiSysClientDetailsResponse;
import com.simple.common.oauth.start.common.service.ClientDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: Aviator初始化自定义计算规则
 *
 * @author qty
 */
@Slf4j
@Component
public class OauthStartInit implements ApplicationListener<ApplicationReadyEvent>, Ordered {

    @Autowired
    private ClientDetailsService clientDetailsService;

    @Autowired
    private ApplicationProperties properties;

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    // RSA密钥缓存
    private final Map<String, KeyPair> rsaKeyCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        List<ApiSysClientDetailsResponse> list = clientDetailsService.list(properties.getName());
        list.forEach(response -> {
            if(clientAuthInfo.getClient()){
                // 从PEM格式恢复密钥对
                KeyPair keyPair = CryptoUtil.restoreKeyPair(response.getRsaPublic(), response.getRsaPrivate());
                rsaKeyCache.put(response.getClientId(), keyPair);
            }
        });
    }

    @Override
    public int getOrder() {
        return SimpleOrder.Oauth.getOrder();
    }
}
