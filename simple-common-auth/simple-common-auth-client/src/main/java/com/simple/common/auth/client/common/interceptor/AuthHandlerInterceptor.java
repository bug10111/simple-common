package com.simple.common.auth.client.common.interceptor;

import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.manager.auth.WhiteManager;
import com.simple.common.auth.client.common.process.AuthProcess;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.IPUtils;
import com.simple.common.core.utils.UrlRulesUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: 权限拦截器
 *
 * @author qty
 */
@Slf4j
@Component
public class AuthHandlerInterceptor implements HandlerInterceptor {

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    /**
     * 鉴权责任链（按 AuthProcess#getProcess().getOrdered() 升序执行）。
     * required = false：极端裁剪场景（无任何 AuthProcess bean）允许启动，但会记一次告警。
     */
    @Autowired(required = false)
    private List<AuthProcess> authProcesses;

    @Autowired
    private WhiteManager whiteManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //拦截器取到请求先进行判断，如果是OPTIONS请求，则放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        //获取当前IP
        String ipAddr = IPUtils.getIpAddr(request);

        //获取当前请求地址
        String path = request.getRequestURI();

        //不需要登录
        if (!clientAuthInfo.getLogin()) {
            // 默认关闭鉴权为显式配置开关（openLogin()），但必须让误配置可被运维发现：只告警一次
            warnSkipOnce("login");
            return HandlerInterceptor.super.preHandle(request, response, handler);
        }

        //优先ip白名单
        if (clientAuthInfo.isIPWhitelist()) {
            if (whiteManager.checkWhiteIp(path, ipAddr)) {
                return HandlerInterceptor.super.preHandle(request, response, handler);
            }
        }

        //白名单放行
        if (UrlRulesUtils.matches(path, clientAuthInfo.getWhiteMap().keySet())) {
            if (log.isDebugEnabled()) {
                log.debug("URL==>[{}]请求被放行！请求不需登录，且在白名单内！", path);
            }
            return HandlerInterceptor.super.preHandle(request, response, handler);
        }

        //鉴权
        else {

            //获取token
            String header = request.getHeader(TokenConstant.Authorization);
            if (header != null && header.toLowerCase().startsWith(TokenConstant.bearer.toLowerCase())) {
                header = header.substring(TokenConstant.bearer.length()).trim();
            } else {
                // 不支持的认证方案与「未携带token」区分开，便于客户端排障
                if (header != null) {
                    log.warn("URL==>[{}]请求被拦截！Authorization 头未使用 Bearer 方案，无法识别凭证", path);
                }
                header = null;
            }

            //没有携带token
            if (ObjUtil.isEmpty(header)) {
                log.error("URL==>[{}]请求被拦截！请求没有携带token，且不处于白名单内！", path);
                AssertUtils.error(LoginException.RE_LOGIN_EXPIRED);
            }

            //携带token，进行合法性校验
            else {
                List<AuthProcess> processes = authProcesses == null ? Collections.emptyList() : new java.util.ArrayList<>(authProcesses);
                // 按 getOrdered() 升序执行（CHECK_TOKEN(1) -> CHECK_SCOPE(2) -> CHECK_ROLE(3)），
                // 保证角色/范围校验依赖的 ThreadLocal 用户上下文先于其构建（排序副本，不动注入集合）
                processes.sort(java.util.Comparator.comparingInt(p -> p.getProcess().getOrdered()));
                try {
                    for (AuthProcess process : processes) {
                        if (process.getProcess().isExecute()) {
                            process.execute(request, response, header, path, ipAddr);
                        }
                    }
                } catch (Exception e) {
                    // preHandle 抛出异常时 afterCompletion 不会被调用（拦截器索引未推进），
                    // 必须在此清理 ThreadLocal，防止线程池复用导致上一个请求的用户身份泄漏到下一个请求
                    LoginUserUtils.remove();
                    throw e;
                }
            }
            if (log.isDebugEnabled()) {
                // 只记录用户与接口，严禁输出凭证本身（token 入日志可被重放）
                log.debug("用户 [{}] ==> 接口 [{}] ", LoginUserUtils.getUserTemporary().getUserId(), path);
            }

            // 放行请求
            return HandlerInterceptor.super.preHandle(request, response, handler);
        }
    }

    /**
     * 鉴权被配置关闭时的一次性告警（fail-open 可见化）。
     */
    private static volatile boolean loginWarned = false;

    private static void warnSkipOnce(String flag) {
        if (!loginWarned) {
            synchronized (AuthHandlerInterceptor.class) {
                if (!loginWarned) {
                    loginWarned = true;
                    log.warn("鉴权拦截器检测到 login={}：本服务当前对所有请求跳过登录校验（@HasAuthority 亦不生效）。" +
                            "若非有意为之，请检查 ClientAuthInfo#openLogin() 配置", flag);
                }
            }
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserUtils.remove();
    }
}
