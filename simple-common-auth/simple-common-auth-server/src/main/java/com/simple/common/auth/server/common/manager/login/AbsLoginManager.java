package com.simple.common.auth.server.common.manager.login;

import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.server.common.entity.AbsUserDetails;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.process.LoginErrorProcess;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.IPUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 登录管理器抽象基类。
 * <p>
 * 提供统一的登录流程模板，包括登录失败次数校验、具体登录逻辑执行、登录成功清理等。
 * 子类只需实现 {@link #doLogin(ClientDetails, Object)} 方法即可。
 * </p>
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Component
 * public class PwdLoginManager extends AbsLoginManager {
 *     @Override
 *     protected AbsUserDetails doLogin(ClientDetails clientDetails, Object adapter) {
 *         // 1. 验证账号是否存在
 *         // 2. 验证密码是否正确
 *         // 3. 检查账号状态
 *         // 4. 构建并返回用户详情
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
@Slf4j
public abstract class AbsLoginManager implements LoginManager {

    @Autowired(required = false)
    protected List<LoginErrorProcess> loginErrorProcesses;

    /**
     * 校验登录失败次数（责任链模式）
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     */
    protected void checkErrorNum(ClientDetails clientDetails, Object adapter) {
        String ip = getIp();

        // 判空处理，避免无处理器时 NPE
        if (loginErrorProcesses == null || loginErrorProcesses.isEmpty()) {
            return;
        }

        loginErrorProcesses.stream().filter(p -> p.getProcess().isExecute()).forEach(process -> {
            if (!process.checkErrorNum(clientDetails, adapter, ip)) {
                AssertUtils.error(LoginException.LOGIN_ERROR_NUM);
            }
        });
    }

    /**
     * 登录失败处理（责任链模式）
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     */
    protected void loginError(ClientDetails clientDetails, Object adapter) {
        String ip = getIp();

        // 记录失败日志，区分不同情况
        if (adapter != null) {
            log.warn("登录失败，账号凭证：[{}]，IP：[{}]", adapter.toString(), ip);
        } else {
            log.warn("登录失败，未提供账号凭证，IP：[{}]", ip);
        }

        if (loginErrorProcesses != null && !loginErrorProcesses.isEmpty()) {
            loginErrorProcesses.stream().filter(p -> p.getProcess().isExecute()).forEach(process -> process.recordError(clientDetails, adapter, ip));
        }
    }

    /**
     * 登录成功，清除失败记录
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     */
    protected void loginSuccess(ClientDetails clientDetails, Object adapter) {
        String ip = getIp();

        if (loginErrorProcesses != null && !loginErrorProcesses.isEmpty()) {
            loginErrorProcesses.stream().filter(p -> p.getProcess().isExecute()).forEach(process -> process.clearError(clientDetails, adapter, ip));
        }
    }

    /**
     * 获取当前请求IP
     *
     * @return IP地址
     */
    protected String getIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return IPUtils.getIpAddr(request);
        }
        return IPUtils.UNKNOWN;
    }

    /**
     * 模板方法：执行完整的登录流程
     * <p>
     * 此方法由框架调用，按顺序执行：
     * 1. 校验登录失败次数
     * 2. 执行具体登录逻辑（子类实现）
     * 3. 登录成功，清除失败记录
     * 4. 登录失败，记录失败次数并抛出异常
     * </p>
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     * @return 用户详情
     */
    public final AbsUserDetails login(ClientDetails clientDetails, Object adapter) {
        try {
            // 1. 校验登录失败次数
            checkErrorNum(clientDetails, adapter);

            // 2. 执行具体登录逻辑（子类实现）
            AbsUserDetails userDetails = doLogin(clientDetails, adapter);

            // 3. 登录成功，清除失败记录
            loginSuccess(clientDetails, adapter);

            return userDetails;
        } catch (Exception e) {
            // 4. 登录失败，记录失败次数
            loginError(clientDetails, adapter);
            // 5. 重新抛出异常（保持原有异常信息）
            throw e;
        }
    }

    /**
     * 执行具体登录逻辑（子类必须实现）
     * <p>
     * 子类在此方法中实现具体的认证逻辑，如：
     * - 验证账号是否存在
     * - 验证密码是否正确
     * - 检查账号状态
     * - 查询用户角色和权限
     * - 构建并返回用户详情
     * </p>
     * <p>
     * 注意：
     * 1. 此方法不需要调用 checkErrorNum、loginSuccess 和 loginError
     * 2. 所有异常都会由模板方法统一捕获并调用 loginError
     * 3. 子类只需抛出异常即可，框架会自动处理失败记录
     * </p>
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     * @return 用户详情
     */
    protected abstract AbsUserDetails doLogin(ClientDetails clientDetails, Object adapter);
}