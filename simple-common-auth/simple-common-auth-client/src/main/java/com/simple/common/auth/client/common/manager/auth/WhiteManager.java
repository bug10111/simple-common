package com.simple.common.auth.client.common.manager.auth;

/**
 * Created with IntelliJ IDEA
 * Description: 白名单接口
 *
 * @author qty
 */
public interface WhiteManager {

    /**
     * 校验是否直接放行
     *
     * @param path   当前请求url
     * @param ipAddr 当前请求ip
     */
    void checkWhiteIp(String path, String ipAddr);

}
