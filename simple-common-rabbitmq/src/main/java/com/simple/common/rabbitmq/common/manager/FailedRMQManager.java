package com.simple.common.rabbitmq.common.manager;

/**
 * Created with IntelliJ IDEA
 * Description: 消费失败处理接口
 *
 * @author qty
 */
public interface FailedRMQManager {

    /**
     * 获取失败次数
     *
     * @param failedKey 标志key
     */
    Long getFailedSum(String failedKey);

    /**
     * 失败次数加一
     *
     * @param failedKey 标志key
     */
    Long increment(String failedKey);

    /**
     * 删除次数
     */
    void delete(String failedKey);

    /**
     * 消费失败的key
     * 格式：消费计数 ：所属队列名 ：全局消息ID
     *
     * @param calculation 标志，等同于redis包名
     * @param queue       queue
     * @param msgId       自定义消息id
     */
    String getFailedMqRedisKey(String calculation, String queue, String msgId);

}