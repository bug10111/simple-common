package com.simple.common.auth.client.common.service;

/**
 * CSRF 服务接口。
 * <p>
 * 修复说明：增加 consume 参数，用于控制校验后是否删除 token。
 *
 * @author qty
 */
public interface CsrfService {

    /**
     * 保存 CSRF Token
     */
    void saveToken(String userId, String path, String token);

    /**
     * 获取CSRF token
     *
     * @return token
     */
    String getToken(String userId, String path);

    /**
     * 删除CSRF token
     */
    void removeToken(String userId, String path);

    /**
     * 校验token
     *
     * @param userId 用户id
     * @param path   路径
     * @param token  token
     * @param consume 是否在校验后立即删除token
     */
    void checkToken(String userId, String path, String token, boolean consume);

}