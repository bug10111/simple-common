package com.simple.common.auth.client.common.config;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;

/**
 * Created with IntelliJ IDEA
 * 客户端权限配置基类
 *
 * @author qty
 */
public abstract class AbsClientAuthConfig {

    /**
     * 客户端权限配置
     *
     * @param clientAuthInfo 客户端权限信息
     */
    protected abstract void configure(ClientAuthInfo clientAuthInfo);

}
