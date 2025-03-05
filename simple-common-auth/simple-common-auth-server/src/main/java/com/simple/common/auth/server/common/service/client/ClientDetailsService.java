package com.simple.common.auth.server.common.service.client;

import com.simple.common.auth.server.common.entity.ClientDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/**
 * Created with IntelliJ IDEA
 * 客户端信息处理接口
 *
 * @author 兄台丶请冷静
 */
public interface ClientDetailsService {

    /**
     * 获取客户端信息
     *
     * @param header
     * @return
     */
    ClientDetails getClientDetails(String header);

    /**
     * 获取客户端token
     *
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     */
    String getClientToken(String clientId, String clientSecret);

    /**
     * 获取客户端信息
     */
    default ClientDetails getClientDetails(HttpServletRequest request) {
        // 获取请求头中的客户端信息
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return this.getClientDetails(header);
    }
}
