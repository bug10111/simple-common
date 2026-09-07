package com.simple.common.eventbus.common.constants;

/**
 * Created with IntelliJ IDEA
 * Description: 定义事件默认常量
 *
 * @author qty
 */
public final class EventConstant {

    //所有系统
    public static final String TARGET_ALL_X = "event.all";

    //本系统
    public static final String THIS_MACHINE = "event.this";

    //事件模式配置前缀（simple.event.type 决定事件模式，与 EventProperties 的 @ConfigurationProperties 对齐）
    public static final String EVENT_TYPE_PREFIX = "simple.event";

    //同步事件模式（simple.event.type=sync，发布方同线程执行处理器）
    public static final String EVENT_TYPE_SYNC = "sync";

    //MQ 异步事件模式（simple.event.type=mq，处理器经 RabbitMQ 消费线程执行）
    public static final String EVENT_TYPE_MQ = "mq";
}