package com.simple.common.rabbitmq.common.enums;

import com.simple.common.core.common.enums.process.DefaultKindProcess;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Getter
@AllArgsConstructor
public enum RMQKindProcess implements DefaultKindProcess {

    TEST("测试步骤", true, 1),

    ;

    //说明
    private final String label;

    //是否执行
    private final boolean execute;

    //执行顺序
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
