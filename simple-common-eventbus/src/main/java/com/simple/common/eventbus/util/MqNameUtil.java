package com.simple.common.eventbus.util;

import cn.hutool.extra.spring.SpringUtil;

/**
 * Created with IntelliJ IDEA
 * Description: 异步事件队列名称帮助类
 *
 * @author qty
 */
public class MqNameUtil {

    //交换机名称
    public static String exchangeName(String serviceName) {
        return getEnvironment(serviceName) + ".event.ex";
    }

    //延时交换机名称
    public static String delayExchangeName(String serviceName) {
        return getEnvironment(serviceName) + ".event.delay.ex";
    }

    //队列名称
    public static String queueName(String serviceName) {
        return getEnvironment(serviceName) + ".event.queue";
    }

    //key名称
    public static String keyName(String serviceName) {
        return getEnvironment(serviceName) + ".event.key";
    }

    /**
     * 获取队列相关名称前缀
     */
    public static String getEnvironment(String name) {
        return name + "-" + SpringUtil.getActiveProfile();
    }

}
