package com.simple.common.auth.server.service.client;

import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.enums.ClientAttribute;
import com.simple.common.auth.server.common.manager.client.ClientManager;
import com.simple.common.auth.server.common.service.client.ClientDetailsService;
import com.simple.common.core.utils.AssertUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
public abstract class AbsClientDetailsService implements ClientDetailsService {

    @Autowired
    private ClientManager clientManager;

    @Override
    public ClientDetails getClientDetails(String header) {
        // 获取请求头中的客户端信息
        if(log.isDebugEnabled()){
            log.debug("header {}", header);
        }
        if (header == null || !header.startsWith(TokenConstant.basic)) {
            AssertUtils.error("请求头中无client信息");
        }

        // 解析请求头的客户端信息
        Map<ClientAttribute, String> map = clientManager.decryptStr(header);
        ClientDetails clientDetails = checkClientDetails(map.get(ClientAttribute.ClientId), map.get(ClientAttribute.ClientSecret));
        if (clientDetails.getAccessTokenValidity() == 0) {
            clientDetails.setAccessTokenValidity(60 * 60 * 12);
        }
        if (clientDetails.getRefreshTokenValidity() == 0) {
            clientDetails.setRefreshTokenValidity(60 * 60 * 24 * 30);
        }
        return clientDetails;
    }

    @Override
    public String getClientToken(String clientId, String clientSecret) {
        return TokenConstant.basic + clientManager.encrypt(clientId, clientSecret);
    }

    /**
     * 获取客户端信息
     * 需要重写的方法
     *
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     */
    public abstract ClientDetails checkClientDetails(String clientId, String clientSecret);
}
