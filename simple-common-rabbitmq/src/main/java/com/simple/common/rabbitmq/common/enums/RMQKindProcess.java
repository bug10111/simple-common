package com.simple.common.rabbitmq.common.enums;

import com.simple.common.core.common.enums.process.DefaultKindProcess;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * RabbitMQ 消息消费责任链处理器类型枚举
 * <p>
 * 当前仅含占位枚举值 TEST：框架默认不提供业务前置处理节点，责任链在默认装配下为空转。
 * 集成方扩展消息消费前置处理时，在此新增枚举值并实现对应 {@code RabbitMqProcess} 处理器，
 * 通过 execute 控制是否参与执行、order 控制执行顺序（值越小越先执行）。
 * </p>
 *
 * @author qty
 */
@Getter
@AllArgsConstructor
public enum RMQKindProcess implements DefaultKindProcess {

    /**
     * 占位枚举值：无业务含义且默认不执行（execute=false），仅为责任链装配提供默认类型；
     * 集成方新增业务处理节点后此占位值可保留，不会实际执行
     */
    TEST("测试步骤", false, 1),

    ;

    /**
     * 中文说明
     */
    private final String label;

    /**
     * 是否执行
     */
    private final boolean execute;

    /**
     * 执行顺序，值越小越先执行
     */
    private final int order;

    @Override
    public Integer getOrdered() {
        return this.order;
    }

    @Override
    public String getMsg() {
        return this.getLabel();
    }
}