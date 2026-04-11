package com.simple.common.logs.client.common.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Created with IntelliJ IDEA
 * Description: 日志生成接口
 *
 * @author qty
 */
public interface LogService {

    /**
     * 发送日志数据
     */
    void send(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex);

    /**
     * 启动日志客户端
     */
    void start();

    /**
     * 停止日志客户端
     */
    void stop();


}
