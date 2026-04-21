package com.simple.common.oauth.start.common.manager;

import com.simple.common.oauth.start.common.dto.ApiSysClientDetailsResponse;

import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: 客户端秘钥远程获取工具
 *
 * @author qty
 */
public interface ClientDetailsManager {

    /**
     * 获取客户端列表
     *
     * @param server 服务
     */
    List<ApiSysClientDetailsResponse> list(String server);

}
