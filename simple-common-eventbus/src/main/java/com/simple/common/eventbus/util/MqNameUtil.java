package com.simple.common.eventbus.util;

import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA
 * Description: 异步事件队列名称帮助类
 *
 * @author qty
 */
@Slf4j
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
     * <p>存在多个激活 Profile 时按字典序取首个：激活参数顺序不同会让同一服务生成
     * xxx-dev 与 xxx-test 两套拓扑，导致跨环境消息路由错乱与队列重复声明冲突</p>
     * <p>注意：若需精确区分环境，建议通过配置项显式指定环境标识</p>
     */
    public static String getEnvironment(String name) {
        // 服务名/路由目标直接拼接生成 MQ 名称，空白值会产出 "null-dev.event.ex" 这类非法名称，
        // 且可能与其它业务队列/交换机撞名互相消费、极难排查，此处显式拒绝
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("serviceName 不能为空，无法生成 MQ 名称");
        }

        String profile = null;
        String[] activeProfiles;
        try {
            activeProfiles = SpringUtil.getActiveProfiles();
        } catch (Exception e) {
            // Spring 上下文未就绪时（如 @Bean 声明 Queue/Exchange 阶段）直接取环境会 NPE，
            // 回退默认环境并告警，避免拓扑装配阶段直接中断启动
            activeProfiles = null;
            log.warn("Spring 环境未就绪，MQ 名称使用默认环境 default", e);
        }
        if (activeProfiles != null && activeProfiles.length > 0) {
            // 多 Profile 激活顺序不确定，排序后取首个保证同一激活集合下名称稳定
            profile = Arrays.stream(activeProfiles).sorted().findFirst().orElse(null);
        }

        // 若未设置环境，使用默认值防止空字符串
        if (!StringUtils.hasText(profile)) {
            profile = "default";
        }
        return name + "-" + profile;
    }

}
