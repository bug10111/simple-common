package com.simple.common.auth.server.manager.secret;

import cn.hutool.core.util.IdUtil;
import com.simple.common.auth.server.common.manager.secret.SignSecretManager;
import org.springframework.stereotype.Component;

/**
 * 默认签名密钥管理器实现（服务端）
 * <p>
 * 使用随机生成的方式产生API签名密钥。
 * 生成32位随机字符串，适用于HMAC-SHA256签名。
 * </p>
 *
 * <h3>特点：</h3>
 * <ul>
 *   <li>每次调用都生成新的随机密钥</li>
 *   <li>密钥强度符合安全要求（32位）</li>
 *   <li>适用于开发环境和快速部署场景</li>
 * </ul>
 *
 * <h3>生产环境建议：</h3>
 * <p>
 * 如果需要在多实例间共享密钥或从数据库加载密钥，
 * 建议自定义实现 {@link SignSecretManager} 接口。
 * </p>
 *
 * @author qty
 */
@Component
public class DefaultSignSecretManager implements SignSecretManager {

    /**
     * 生成新的签名密钥
     * <p>
     * 使用IdUtil生成32位UUID作为密钥。
     * 每次调用都会生成不同的密钥。
     * </p>
     *
     * @return 32位随机密钥字符串
     */
    @Override
    public String generate() {
        return IdUtil.fastSimpleUUID();
    }
}
