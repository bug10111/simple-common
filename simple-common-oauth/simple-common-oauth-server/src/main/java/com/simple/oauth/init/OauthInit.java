package com.simple.oauth.init;

import com.simple.common.core.common.enums.order.SimpleOrder;
import com.simple.oauth.common.manager.init.OauthInitManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created with IntelliJ IDEA
 * Description: 初始化客户端密钥信息
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Component
public class OauthInit implements ApplicationListener<ApplicationReadyEvent>, Ordered {

    @Autowired
    private OauthInitManager oauthInitManager;

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        oauthInitManager.loadingSecret();
        oauthInitManager.loadingUserAndRole();
        oauthInitManager.auth();
    }

    @Override
    public int getOrder() {
        return SimpleOrder.Oauth.getOrder();
    }
}
