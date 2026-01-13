package com.simple.common.auth.client.common.process;

import com.simple.common.core.common.service.process.BasProcessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Created with IntelliJ IDEA
 * Description: 权限过滤器处理器
 *
 * @author qty
 */
public interface AuthProcess extends BasProcessService {

    /**
     * 执行权限过滤器
     *
     * @param request  请求对象
     * @param response 响应对象 用于设置响应头
     * @param token    登录token
     * @param path     请求地址
     * @param ipAddr   IP地址
     */
    void execute(HttpServletRequest request, HttpServletResponse response, String token, String path, String ipAddr);
}
