package com.simple.common.auth.server.common.manager.client;

import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.enums.ClientAttribute;

import java.util.Map;

/**
 * 客户端管理器接口。
 * <p>
 * 用于管理OAuth客户端信息，包括客户端详情获取和客户端token生成。
 * 默认实现 {@link com.simple.common.auth.server.manager.DefaultClientManager} 从数据库加载客户端配置。
 * </p>
 *
 * @author qty
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