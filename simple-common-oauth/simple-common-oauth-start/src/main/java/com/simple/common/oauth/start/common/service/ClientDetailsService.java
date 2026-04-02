package com.simple.common.oauth.start.common.service;

import com.simple.common.oauth.start.common.dto.ApiSysClientDetailsResponse;

import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: 客户端接口
 *
 * @author 兄台丶请冷静
 */
public interface ClientDetailsService {

    /**
     * 获取客户端列表
     *
     * @param server 服务
     */
    List<ApiSysClientDetailsResponse> list(String server);

}
