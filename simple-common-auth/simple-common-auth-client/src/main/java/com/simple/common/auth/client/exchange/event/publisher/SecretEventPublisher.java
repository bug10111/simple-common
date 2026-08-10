package com.simple.common.auth.client.exchange.event.publisher;

import java.util.List;

/**
 * 密钥事件发布器 — 专职将密钥变更推送到远程客户端。
 * <p>
 * 职责单一：仅负责将密钥变更事件发布到消息队列，广播到目标客户端项目。
 * 不涉及本地缓存，本地缓存由 {@link com.simple.common.auth.client.common.manager.token.TokenManager}
 * 和 {@link com.simple.common.auth.client.common.manager.sign.SignManager} 负责。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>服务端修改项目密钥后，广播到所有客户端</li>
 *   <li>服务端新增项目密钥后，广播通知客户端加载</li>
 * </ul>
 *
 * <h3>典型调用链：</h3>
 * <pre>{@code
 * // 在密钥修改的业务方法中
 * // 1. 本地缓存
 * tokenManager.addSecret(newSecret, projectCode);
 * signManager.addSecret(newSignSecret, projectCode);
 *
 * // 2. 广播到远程客户端
 * secretEventPublisher.broadcastJwtSecret(newSecret, clientProjectCodes);
 * secretEventPublisher.broadcastSignSecret(newSignSecret, clientProjectCodes);
 * }</pre>
 *
 * @author qty
 */
public interface SecretEventPublisher {

    /**
     * 广播JWT密钥到指定客户端项目
     *
     * @param secret             JWT签名密钥
     * @param targetProjectCodes 目标客户端项目编码集合
     */
    void broadcastJwtSecret(String secret, List<String> targetProjectCodes);

    /**
     * 广播签名密钥到指定客户端项目
     *
     * @param secret             签名密钥
     * @param targetProjectCodes 目标客户端项目编码集合
     */
    void broadcastSignSecret(String secret, List<String> targetProjectCodes);
}
