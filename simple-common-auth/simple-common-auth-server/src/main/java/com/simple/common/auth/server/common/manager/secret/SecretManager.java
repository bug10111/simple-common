package com.simple.common.auth.server.common.manager.secret;

/**
 * Created with IntelliJ IDEA
 * Description: 秘钥管理器接口
 *
 * @author qty
 */
public interface SecretManager {

    /**
     * 添加秘钥
     *
     * @param secret 新秘钥
     */
    void addSecret(String secret);

    /**
     * 为指定客户端添加秘钥
     *
     * @param clientId 客户端ID
     * @param secret   新秘钥
     */
    void addSecret(String clientId, String secret);

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