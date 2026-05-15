package com.simple.common.auth.server.common.enums.process;

import com.simple.common.core.common.enums.process.DefaultKindProcess;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 * Description: 登录异常处理流程枚举
 *
 * @author qty
 */
@Getter
@AllArgsConstructor
public enum LoginErrorKindProcess implements DefaultKindProcess {

    IP_ERROR("基于IP的登录失败计数", true, 1);

    private final String msg;
    private final boolean execute;
    private final Integer ordered;

    @Override
    public boolean isExecute() {
        return this.execute;
    }

    @Override
    public Integer getOrdered() {
        return this.ordered;
    }

    @Override
    public String getMsg() {
        return this.msg;
    }

    public String getCode(){
        return name();
    }
}
