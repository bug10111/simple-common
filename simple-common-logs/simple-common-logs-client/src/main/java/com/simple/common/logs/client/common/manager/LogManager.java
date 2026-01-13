package com.simple.common.logs.client.common.manager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Created with IntelliJ IDEA
 * Description: 日志生成接口
 *
 * @author qty
 */
public interface LogManager {

    /**
     * 生成日志
     */
    void create(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex);

}
