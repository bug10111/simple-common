package com.simple.oauth.common.manager.username;

/**
 * Created with IntelliJ IDEA
 * Description: 用户名称缓存接口
 *
 * @author qty
 */
public interface SysUserNameCacheManager {

    /**
     * 缓存
     *
     * @param userId   用户Id
     * @param userName 用户名称
     */
    void put(String userId, String userName);

    /**
     * 获取
     *
     * @param userId 用户Id
     */
    String get(String userId);

}
