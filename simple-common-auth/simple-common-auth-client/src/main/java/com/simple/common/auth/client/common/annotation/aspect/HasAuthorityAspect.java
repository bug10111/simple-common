package com.simple.common.auth.client.common.annotation.aspect;

import com.simple.common.auth.client.common.annotation.HasAuthority;
import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.manager.user.LoginInfoManager;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.utils.AssertUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * 权限注解切面。
 *
 * @author qty
 */
@Slf4j
@Aspect
@Component
public class HasAuthorityAspect {

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Autowired
    private AuthProperties authProperties;

    @Qualifier("clientLoginInfoManager")
    @Autowired
    private LoginInfoManager loginInfoManager;

    /**
     * 前置通知：校验方法上的 @HasAuthority 注解。
     * <p>
     * 超级管理员（loginRole 包含 superAdminRoleKey）直接放行，不参与权限校验。
     * </p>
     *
     * @param joinPoint 切点
     */
    @Before("@annotation(com.simple.common.auth.client.common.annotation.HasAuthority)")
    public void before(JoinPoint joinPoint) {
        boolean login = clientAuthInfo.getLogin();
        boolean authentication = clientAuthInfo.getAuthentication();

        if (login && authentication) {
            // 超级管理员直接放行
            Set<String> loginRole = LoginUserUtils.getUserTemporary().getLoginRole();
            if (loginRole != null && loginRole.contains(authProperties.getSuperAdminRoleKey())) {
                return;
            }

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            HasAuthority hasAuthority = method.getAnnotation(HasAuthority.class);
            String[] value = hasAuthority.value();

            Boolean hasAuth = loginInfoManager.hasAuth(loginRole, value);

            AssertUtils.isTrue(hasAuth, LoginException.INSUFFICIENT_PERMISSIONS);
        }
    }
}