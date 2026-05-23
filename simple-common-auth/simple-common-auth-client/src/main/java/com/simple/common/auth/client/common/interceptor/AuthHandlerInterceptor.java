package com.simple.common.auth.client.common.interceptor;

import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.manager.auth.WhiteManager;
import com.simple.common.auth.client.common.process.AuthProcess;
import com.simple.common.auth.client.common.properties.AuthProperties;
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

    @Autowired
    private List<AuthProcess> list;

    @Autowired
    private WhiteManager whiteManager;

    @Autowired
    private AuthProperties authProperties;

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
            return HandlerInterceptor.super.preHandle(request, response, handler);
        }

        //优先ip白名单
        if (clientAuthInfo.isIPWhitelist()) {
            if(whiteManager.checkWhiteIp(path, ipAddr)){
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
                header = null;
            }

            //没有携带token
            if (ObjUtil.isEmpty(header)) {
                log.error("URL==>[{}]请求被拦截！请求没有携带token，且不处于白名单内！", path);
                AssertUtils.error(LoginException.RE_LOGIN_EXPIRED);
            }

            //携带token，进行合法性校验
            else {

                for (AuthProcess process : list) {
                    if (process.getProcess().isExecute()) {
                        process.execute(request, response, header, path, ipAddr);
                    }
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("用户 [{}] ==> 接口 [{}] ==> token [{}] ", LoginUserUtils.getUserTemporary().getUserId(), path, header);
            }

            // 放行请求
            return HandlerInterceptor.super.preHandle(request, response, handler);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserUtils.remove();
    }
}
