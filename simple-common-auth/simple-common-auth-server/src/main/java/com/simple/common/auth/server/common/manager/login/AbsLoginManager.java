package com.simple.common.auth.server.common.manager.login;

import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.server.common.entity.AbsUserDetails;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.process.LoginErrorProcess;
import com.simple.common.core.exception.DefaultException;
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
     * <p>
     * 失败日志只记凭证类型，不落凭证内容（adapter.toString 可能携带明文密码/验证码）；
     * 单个处理器记录失败不影响其余处理器，也绝不掩盖本次认证失败异常。
     * </p>
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     */
    protected void loginError(ClientDetails clientDetails, Object adapter) {
        String ip = getIp();

        // 记录失败日志，区分不同情况（仅记凭证类型，防止凭证泄入日志）
        if (adapter != null) {
            log.warn("登录失败，账号凭证类型：[{}]，IP：[{}]", adapter.getClass().getSimpleName(), ip);
        } else {
            log.warn("登录失败，未提供账号凭证，IP：[{}]", ip);
        }

        if (loginErrorProcesses != null && !loginErrorProcesses.isEmpty()) {
            loginErrorProcesses.stream().filter(p -> p.getProcess().isExecute()).forEach(process -> {
                try {
                    process.recordError(clientDetails, adapter, ip);
                } catch (Exception e) {
                    log.error("记录登录失败次数异常，process: {}", process.getProcess().getMsg(), e);
                }
            });
        }
    }

    /**
     * 登录成功，清除失败记录（尽力而为：单个处理器清理失败不中断登录成功流程）
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     */
    protected void loginSuccess(ClientDetails clientDetails, Object adapter) {
        String ip = getIp();

        if (loginErrorProcesses != null && !loginErrorProcesses.isEmpty()) {
            loginErrorProcesses.stream().filter(p -> p.getProcess().isExecute()).forEach(process -> {
                try {
                    process.clearError(clientDetails, adapter, ip);
                } catch (Exception e) {
                    log.error("清除登录失败记录异常，process: {}", process.getProcess().getMsg(), e);
                }
            });
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
     * 1. 校验登录失败次数（置于 try 外：锁定拒绝不作为一次新的失败计次，避免锁定窗口被无限顺延）
     * 2. 执行具体登录逻辑（子类实现）
     * 3. 业务认证失败（{@link DefaultException}，业务码由 LoginException 等枚举承载）记录失败次数并原样抛出
     * 4. 系统内部异常（缓存/DB 等基础设施故障、NPE 等）不计入登录失败次数，原样上抛
     * 5. 登录成功，清除失败记录
     * </p>
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     * @return 用户详情
     */
    public final AbsUserDetails login(ClientDetails clientDetails, Object adapter) {
        // 1. 校验登录失败次数（锁定拒绝直接抛出，不重复计次）
        checkErrorNum(clientDetails, adapter);

        AbsUserDetails userDetails;
        try {
            // 2. 执行具体登录逻辑（子类实现）
            userDetails = doLogin(clientDetails, adapter);
        } catch (DefaultException e) {
            // 3. 业务认证失败（框架断言统一抛出 DefaultException，业务码由 LoginException 等枚举承载）：
            //    记录失败次数并原样抛出
            //   （loginError 内部已对记录动作自身的异常兜底，不会掩盖本次认证失败）
            loginError(clientDetails, adapter);
            throw e;
        } catch (Exception e) {
            // 4. 系统内部异常：不计入登录失败次数（防止基础设施故障把凭证正确的用户推向锁定），
            //    原样上抛由全局异常处理兜底
            log.error("登录流程发生系统异常，IP：[{}]", getIp(), e);
            throw e;
        }

        // 5. 登录成功，清除失败记录
        loginSuccess(clientDetails, adapter);
        return userDetails;
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
     * 2. 业务认证失败请通过 AssertUtils 断言抛出 {@link DefaultException}（业务码用 LoginException 等枚举承载），
     *    框架会统一捕获并记录失败次数；其他运行时异常视为系统内部异常，不计入失败次数
     * 3. 返回的用户详情不能为 null
     * </p>
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     * @return 用户详情
     */
    protected abstract AbsUserDetails doLogin(ClientDetails clientDetails, Object adapter);
}
