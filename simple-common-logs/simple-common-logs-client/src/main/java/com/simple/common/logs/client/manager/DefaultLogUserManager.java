package com.simple.common.logs.client.manager;

import com.simple.common.logs.client.common.manager.LogUserManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 日志用户信息管理器默认实现。
 * <p>
 * 默认返回占位用户（"测试用户ID"/"测试用户名"）并输出 warn 提示。
 * 生产环境必须继承本类并覆写 {@link #loginUserId()} 与 {@link #loginNickName()}，
 * 从登录上下文返回真实用户身份，否则日志将记录占位用户。
 * 子类注册为 Bean 后，须以 {@code @Primary} 标注子类或以配置排除本默认实现，
 * 避免同类型出现两个 Bean 导致注入冲突。
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
@Component
public class DefaultLogUserManager implements LogUserManager {

    @Override
    public String loginNickName() {
        log.warn("当前使用默认占位用户名，生产环境必须覆写 loginNickName 提供真实用户昵称");
        return "测试用户名";
    }

    @Override
    public String loginUserId() {
        log.warn("当前使用默认占位用户ID，生产环境必须覆写 loginUserId 提供真实登录用户ID");
        return "测试用户ID";
    }
}
