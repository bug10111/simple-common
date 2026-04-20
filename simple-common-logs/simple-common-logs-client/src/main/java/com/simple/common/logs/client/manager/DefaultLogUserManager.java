package com.simple.common.logs.client.manager;

import com.simple.common.logs.client.common.manager.LogUserManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 日志用户信息管理器默认实现。
 * <p>
 * 默认返回 null，不提供用户信息。
 * 如需在日志中记录用户身份信息，请继承此类并重写 {@link #getUserId()} 方法。
 * </p>
 *
 * <h3>扩展示例：</h3>
 * <pre>{@code
 * @Component
 * public class CustomLogUserManager extends DefaultLogUserManager {
 *     @Override
 *     public String getUserId() {
 *         // 从登录上下文获取用户ID
 *         return LoginUserUtils.getUserId();
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultLogUserManager implements LogUserManager {

    @Override
    public String loginNickName() {
        log.warn("请实现LogUserManager提供用户名和ID");
        return "测试用户名";
    }

    @Override
    public String loginUserId() {
        log.warn("请实现LogUserManager提供用户名和ID");
        return "测试用户ID";
    }
}