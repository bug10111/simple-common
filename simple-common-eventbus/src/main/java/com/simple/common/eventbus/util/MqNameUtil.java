package com.simple.common.eventbus.util;

import cn.hutool.extra.spring.SpringUtil;
import org.springframework.util.StringUtils;

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
     * 获取队列相关名称前缀，格式：服务名-环境标识
     */
    public static String getEnvironment(String name) {
        String profile = null;
        try {
            profile = SpringUtil.getActiveProfile();
        } catch (Exception e) {
            // 忽略异常，使用默认值
        }
        // 若未设置环境，使用默认值防止空字符串
        if (!StringUtils.hasText(profile)) {
            profile = "default";
        }
        return name + "-" + profile;
    }

}