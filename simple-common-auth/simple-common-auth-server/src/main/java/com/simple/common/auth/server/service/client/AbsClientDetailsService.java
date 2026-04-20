package com.simple.common.auth.server.service.client;

import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.enums.ClientAttribute;
import com.simple.common.auth.server.common.manager.client.ClientManager;
import com.simple.common.auth.server.common.service.client.ClientDetailsService;
import com.simple.common.auth.server.common.properties.AuthServerProperties;
import com.simple.common.core.utils.AssertUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * 客户端详情服务抽象基类。
 * <p>
 * 实现 {@link ClientDetailsService} 接口，提供客户端Token获取的标准流程。
 * 业务系统需要继承此类并实现 {@link #checkClientDetails(String, String)} 方法来
 * 定义客户端信息的获取逻辑（如从数据库、缓存等获取）。
 * </p>
 *
 * <h3>扩展指南：</h3>
 * <p>
 * 如需自定义客户端信息获取方式，可继承此类并实现 {@code getClientDetails} 方法：
 * </p>
 * <pre>{@code
 * public class MyClientDetailsService extends AbsClientDetailsService {
 *     @Autowired
 *     private ClientRepository clientRepository;
 *
 *     @Override
 *     protected ClientDetails getClientDetails(String clientId, String clientSecret) {
 *         SysClientDetails entity = clientRepository.findByClientId(clientId);
 *         AssertUtils.isTrue(entity != null, "客户端不存在");
 *         AssertUtils.isTrue(entity.getClientSecret().equals(clientSecret), "客户端密钥错误");
 *         return convertToClientDetails(entity);
 *     }
 * }
 * }</pre>
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