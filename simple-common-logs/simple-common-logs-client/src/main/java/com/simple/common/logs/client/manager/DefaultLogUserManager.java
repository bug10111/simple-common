package com.simple.common.logs.client.manager;

import com.simple.common.logs.client.common.manager.LogUserManager;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 日志用户信息管理器默认实现。
 * <p>
 * 默认返回占位用户（"测试用户ID"/"测试用户名"）并仅输出一次 warn 提示。
 * 生产环境必须继承本类并覆写 {@link #loginUserId()} 与 {@link #loginNickName()}，
 * 从登录上下文返回真实用户身份，否则日志将记录占位用户。
 * 本类不再标注 {@code @Component}：由 {@code LogClientConfig} 以
 * {@code @Bean @ConditionalOnMissingBean(LogUserManager.class)} 注册，
 * 使用方注册任意 LogUserManager 实现后本默认 Bean 自动退避，无需 @Primary。
 * </p>
 *
 * <h3>扩展示例：</h3>
 * <pre>{@code
 * @Component
 * public class CustomLogUserManager extends DefaultLogUserManager {
 *     @Override
 *     public String loginUserId() {
 *         // 从登录上下文获取用户ID
 *         return LoginUserUtils.getUserTemporary().getUserId();
 *     }
 *
 *     @Override
 *     public String loginNickName() {
 *         // 从登录上下文获取用户昵称
 *         return LoginUserUtils.getUserTemporary().getNickName();
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
@Slf4j
public class DefaultLogUserManager implements LogUserManager {

    /**
     * 占位用户告警只发一次：本实现会被每条日志事件高频调用，
     * 每次 warn 既放大日志量又损耗性能，且告警可能来自日志管线自身造成递归放大
     */
    private final AtomicBoolean placeholderWarned = new AtomicBoolean(false);

    private void warnPlaceholderOnce(String message) {
        if (placeholderWarned.compareAndSet(false, true)) {
            log.warn(message);
        }
    }

    @Override
    public String loginNickName() {
        warnPlaceholderOnce("当前使用默认占位用户名，生产环境必须覆写 loginNickName 提供真实用户昵称");
        return "测试用户名";
    }

    @Override
    public String loginUserId() {
        warnPlaceholderOnce("当前使用默认占位用户ID，生产环境必须覆写 loginUserId 提供真实登录用户ID");
        return "测试用户ID";
    }
}
