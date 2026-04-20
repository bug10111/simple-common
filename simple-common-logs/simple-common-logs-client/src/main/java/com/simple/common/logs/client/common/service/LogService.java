package com.simple.common.logs.client.common.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 日志服务接口。
 * <p>
 * 提供日志客户端的启动和日志发送功能。
 * 默认实现 {@link com.simple.common.logs.client.service.DefaultLogService} 通过TCP协议
 * 将日志数据发送到日志服务器。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Autowired
 * private LogService logService;
 *
 * // 在应用启动时调用
 * logService.start();
 * }</pre>
 *
 * @author qty
 */
public interface LogService {

    /**
     * 发送日志数据
     */
    void send(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex);

    /**
     * 启动日志客户端。
     * <p>
     * 初始化TCP连接，建立与日志服务器的通信通道。
     * 通常在应用启动时自动调用，无需手动调用。
     * </p>
     */
    void start();

    /**
     * 停止日志客户端
     */
    void stop();


}