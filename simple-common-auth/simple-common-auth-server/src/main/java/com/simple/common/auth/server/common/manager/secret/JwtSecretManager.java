package com.simple.common.auth.server.common.manager.secret;

/**
 * JWT秘钥管理器接口。
 * <p>
 * 用于管理JWT令牌的签名秘钥，包括秘钥的初始化、获取和验证。
 * 默认实现 {@link com.simple.common.auth.server.manager.DefaultJwtSecretManager} 支持秘钥的动态更新。
 * </p>
 *
 * @author qty
 */
public interface JwtSecretManager {

    /**
     * 添加秘钥
     *
     * @param secret 新秘钥
     */
    void addSecret(String secret);

    /**
     * 修改秘钥
     *
     * @param oldSecret 旧秘钥
     * @param newSecret 新秘钥
     */
    void updateSecret(String oldSecret, String newSecret);

    /**
     * 获取当前秘钥
     *
     * @return 当前秘钥
     */
    String getCurrentSecret();

    /**
     * 验证秘钥是否存在
     *
     * @param secret 秘钥
     * @return 是否存在
     */
    boolean existsSecret(String secret);
}