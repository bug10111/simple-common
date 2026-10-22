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
    private final int ordered;


    /**
     * 稳定的缓存键段（登录失败计数 Redis 前缀成分）：显式字面量，
     * 不随枚举名重命名漂移，保证既有计数器跨版本有效
     */
    @Override
    public String getCode() {
        return "IP_ERROR";
    }
}