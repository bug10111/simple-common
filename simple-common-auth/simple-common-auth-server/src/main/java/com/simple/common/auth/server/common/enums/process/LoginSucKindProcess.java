package com.simple.common.auth.server.common.enums.process;

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
public enum LoginSucKindProcess implements DefaultKindProcess {

    SAVE_INFO("保存用户信息", true, 1),
    LOGIN_LOG("登录日志", true, 2),

    ;

    //说明
    private final String label;

    //是否执行
    private final boolean execute;

    //执行顺序
    private final int order;

    @Override
    public int getOrdered() {
        return this.order;
    }

    @Override
    public String getMsg() {
        return this.getLabel();
    }

    /**
     * 稳定键段：返回枚举名而非中文标签，避免标签改词漂移派生键
     */
    @Override
    public String getCode() {
        return name();
    }
}