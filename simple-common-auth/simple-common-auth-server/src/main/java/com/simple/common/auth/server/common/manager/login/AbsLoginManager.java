package com.simple.common.auth.server.common.manager.login;

import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.process.LoginErrorProcess;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.IPUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
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
        
        // 按顺序执行所有处理器
        loginErrorProcesses.stream()
                .filter(p -> p.getProcess().isExecute())
                .forEach(process -> {
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
        
        // 按顺序执行所有处理器
        loginErrorProcesses.stream()
                .filter(p -> p.getProcess().isExecute())
                .forEach(process -> process.recordError(clientDetails, adapter, ip));
        
        AssertUtils.error("账号或者密码错误");
    }

    /**
     * 登录成功，清除失败记录
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     */
    protected void loginSuccess(ClientDetails clientDetails, Object adapter) {
        String ip = getIp();
        
        // 清除所有失败记录
        loginErrorProcesses.stream()
                .filter(p -> p.getProcess().isExecute())
                .forEach(process -> process.clearError(clientDetails, adapter, ip));
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


}