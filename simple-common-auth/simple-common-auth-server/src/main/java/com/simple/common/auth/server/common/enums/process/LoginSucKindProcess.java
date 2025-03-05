package com.simple.common.auth.server.common.enums.process;

import com.simple.common.core.common.enums.process.DefaultKindProcess;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Getter
@AllArgsConstructor
public enum LoginSucKindProcess implements DefaultKindProcess {

    SAVE_INFO("保存用户信息", true, 1),
    LOGIN_LOG("登陆日志", true, 2),

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
