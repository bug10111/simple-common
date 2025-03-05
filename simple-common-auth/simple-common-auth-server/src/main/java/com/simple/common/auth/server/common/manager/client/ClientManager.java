package com.simple.common.auth.server.common.manager.client;

import com.simple.common.auth.server.common.enums.ClientAttribute;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 客户端工具接口
 *
 * @author 兄台丶请冷静
 */
public interface ClientManager {

    /**
     * 获取客户端token
     *
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     */
    String encrypt(String clientId, String clientSecret);

    /**
     * 获取客户端ID和密钥
     *
     * @param header 头的完整字符串
     */
    Map<ClientAttribute, String> decryptStr(String header);

}
