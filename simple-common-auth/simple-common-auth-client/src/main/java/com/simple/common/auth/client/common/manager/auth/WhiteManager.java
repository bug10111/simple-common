package com.simple.common.auth.client.common.manager.auth;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;

/**
 * 白名单管理器接口。
 * <p>
 * 用于检查请求路径是否在白名单中，白名单路径无需进行权限校验。
 * 默认实现 {@link com.simple.common.auth.client.manager.auth.DefaultWhiteManager} 返回 false，
 * 如需自定义白名单逻辑，请继承 {@link com.simple.common.auth.client.manager.auth.DefaultWhiteManager} 并重写 {@link #checkWhite} 方法。
 * </p>
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
