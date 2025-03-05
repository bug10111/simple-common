package com.simple.common.auth.client.common.enums.process;

import com.simple.common.core.common.enums.process.DefaultKindProcess;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 * Description: 用户校验控制器
 *
 * @author 兄台丶请冷静
 */
@Getter
@AllArgsConstructor
public enum AuthInterceptorKindProcess implements DefaultKindProcess {

    CHECK_TOKEN("检查token合法性", true, 1),
    CHECK_SCOPE_AUTH("检查授权范围", true, 2),

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
