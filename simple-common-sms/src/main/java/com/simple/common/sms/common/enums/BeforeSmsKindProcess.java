package com.simple.common.sms.common.enums;

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
public enum BeforeSmsKindProcess implements DefaultKindProcess {

    TIME_INTERVAL_PROCESS("校验发送时间间隔", true, 1),
    IP_PROCESS("根据ip校验发送次数", true, 2),
    PHONE_PROCESS("根据手机号校验发送次数", true, 3),

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
