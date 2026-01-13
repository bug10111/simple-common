package com.simple.common.auth.client.common.service;

/**
 * Created with IntelliJ IDEA
 * Description: CSRF 服务接口
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
     */
    void checkToken(String userId, String path, String token);

}
