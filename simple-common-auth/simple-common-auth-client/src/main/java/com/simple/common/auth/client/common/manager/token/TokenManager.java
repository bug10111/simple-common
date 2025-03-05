package com.simple.common.auth.client.common.manager.token;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * token创建的接口
 *
 * @author 兄台丶请冷静
 */
public interface TokenManager {

    /**
     * 创建token
     *
     * @param headers 头部信息
     * @param payload 载荷
     * @return token
     */
    String create(Map<String, Object> headers, Map<String, Object> payload);

    /**
     * 合法性校验，并返回有效载荷
     *
     * @param token     token
     * @param isRefresh 是否失刷新token
     */
    Map<String, Object> check(String token, boolean isRefresh);

    /**
     * 创建token
     *
     * @param payload 载荷
     * @return token
     */
    default String create(Map<String, Object> payload) {
        return create(null, payload);
    }

}
