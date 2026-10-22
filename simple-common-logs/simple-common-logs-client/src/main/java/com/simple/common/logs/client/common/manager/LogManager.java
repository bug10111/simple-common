package com.simple.common.logs.client.common.manager;

import com.simple.common.logs.proto.common.event.LogDataEvent;

/**
 * 日志发送管理器接口。
 * <p>
 * 负责将应用产生的日志数据发送到日志服务器。
 * 支持异步批量发送,提高性能并减少对业务的影响。
 * 默认实现 {@link com.simple.common.logs.client.manager.BufferedLogManager} 基于缓冲队列实现批量发送。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>操作日志：记录用户的增删改查操作</li>
 *   <li>访问日志：记录API接口的调用情况</li>
 *   <li>异常日志：记录系统异常和错误信息</li>
 * </ul>
 *
 * @author qty
 */
public interface LogManager {

    /**
     * 发送日志数据
     * <p>
     * 将单条日志数据加入发送队列。
     * 实际发送可能是异步批量的,取决于具体实现。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 构建日志数据
     * LogDataEvent logData = LogDataEvent.newBuilder()
     *     .setUserId("123")
     *     .setUserName("张三")
     *     .setOperation("创建订单")
     *     .setModule("订单管理")
     *     .setIp("192.168.1.100")
     *     .setCreateTime(System.currentTimeMillis())
     *     .build();
     * 
     * // 发送日志
     * boolean success = logManager.send(logData);
     * }</pre>
     *
     * @param logData 日志数据对象,包含用户信息、操作内容等;不能为 null,传入 null 将抛出 NullPointerException
     * @return true 表示成功加入发送队列,false 表示队列已满或发送失败(该条日志将被降级或丢弃,不会阻塞调用线程)
     *         <p>本方法必须是线程安全的,允许业务线程并发调用。</p>
     */
    boolean send(LogDataEvent logData);

    /**
     * 启动日志客户端
     * <p>
     * 初始化日志发送器,建立与日志服务器的连接。
     * 该方法通常在应用启动时由框架自动调用。
     * </p>
     * <p>该方法应为幂等操作:重复调用不得重复创建发送线程或重复初始化资源;
     * 调用前 send() 可能不可用。实现不应向调用方抛出启动异常。</p>
     */
    void start();

    /**
     * 停止日志客户端
     * <p>
     * 关闭日志发送器,尝试刷新缓冲区中剩余的日志数据。
     * </p>
     * <p>该方法会阻塞直到剩余数据发送完成或超时;超时后未发送的数据按实现策略
     * 降级持久化或丢弃。调用后不支持再次 {@link #start()}。</p>
     */
    void stop();

}