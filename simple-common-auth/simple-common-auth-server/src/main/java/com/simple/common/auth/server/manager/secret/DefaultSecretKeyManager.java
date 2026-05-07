package com.simple.common.auth.server.manager.secret;

import com.simple.common.auth.client.util.JJwtUtils;
import com.simple.common.auth.server.common.manager.secret.SecretKeyManager;
import org.springframework.stereotype.Component;

/**
 * 默认JWT签名密钥管理器实现（服务端）
 * <p>
 * 使用随机生成的方式产生JWT签名密钥。
 * 生成64位随机字符串，适用于JJWT和Hutool JWT。
 * </p>
 *
 * <h3>特点：</h3>
 * <ul>
 *   <li>每次调用都生成新的随机密钥</li>
 *   <li>密钥强度符合安全要求（64位）</li>
 *   <li>适用于开发环境和快速部署场景</li>
 * </ul>
 *
 * <h3>生产环境建议：</h3>
 * <p>
 * 如果需要在多实例间共享密钥或从数据库加载密钥，
 * 建议自定义实现 {@link SecretKeyManager} 接口。
 * </p>
 *
 * @author qty
 */
@Component
public class DefaultSecretKeyManager implements SecretKeyManager {

    /**
     * 生成新的JWT签名密钥
     * <p>
     * 使用JJwtUtils生成64位随机字符串作为密钥。
     * 每次调用都会生成不同的密钥。
     * </p>
     *
     * @return 64位随机密钥字符串
     */
    @Override
    public String generate() {
        return JJwtUtils.createSecret();
    }
}
