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
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 权限注解切面。
 *
 * @author qty
 */
@Slf4j
@Aspect
@Component
public class HasAuthorityAspect {

    /**
     * 鉴权开关被配置关闭时的一次性告警标记（fail-open 必须可被运维发现）。
     */
    private static final AtomicBoolean SKIP_WARNED = new AtomicBoolean(false);

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Autowired
    private AuthProperties authProperties;

    @Qualifier(LoginInfoManager.client_manager_name)
    @Autowired
    private LoginInfoManager loginInfoManager;

    /**
     * 前置通知：校验方法上的 @HasAuthority 注解。
     * <p>
     * 超级管理员（loginRole 包含 superAdminRoleKey）直接放行，不参与权限校验。
     * 权限值判定为 ANY-of：值数组中任一权限命中即通过。
     * </p>
     *
     * @param joinPoint 切点
     */
    @Before("@annotation(com.simple.common.auth.client.common.annotation.HasAuthority)")
    public void before(JoinPoint joinPoint) {
        boolean login = clientAuthInfo.getLogin();
        boolean authentication = clientAuthInfo.getAuthentication();

        if (!login || !authentication) {
            // login/authentication 未开启时注解不生效——这是显式配置开关，但必须可被发现（只告警一次）
            if (SKIP_WARNED.compareAndSet(false, true)) {
                log.warn("@HasAuthority 未生效：login={}, authentication={}，本服务所有 @HasAuthority 接口跳过权限校验。" +
                        "若非有意为之，请检查 ClientAuthInfo#openLogin()/openAuthentication() 配置", login, authentication);
            }
            return;
        }

        // 超级管理员直接放行
        Set<String> loginRole = LoginUserUtils.getUserTemporary().getLoginRole();
        if (loginRole != null && loginRole.contains(authProperties.getSuperAdminRoleKey())) {
            return;
        }

        // 解析最具体的目标类方法（JDK 代理场景 signature.getMethod() 可能落在接口方法上）
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = AopUtils.getMostSpecificMethod(signature.getMethod(), joinPoint.getTarget().getClass());
        HasAuthority hasAuthority = method.getAnnotation(HasAuthority.class);
        // 切点已匹配注解，此处为防御性判空：注解缺失按权限不足处理（fail-closed）
        AssertUtils.notEmpty(hasAuthority, LoginException.INSUFFICIENT_PERMISSIONS, "@HasAuthority 注解解析失败");

        String[] value = hasAuthority.value();
        // 空值数组属于配置错误：按无权限处理，避免静默放行（hasAuth 对空数组返回 true 的语义不适用于注解场景）
        AssertUtils.notEmpty(value, LoginException.INSUFFICIENT_PERMISSIONS, "@HasAuthority 至少需要配置一个权限值");

        Boolean hasAuth = loginInfoManager.hasAuth(loginRole, value);

        AssertUtils.isTrue(Boolean.TRUE.equals(hasAuth), LoginException.INSUFFICIENT_PERMISSIONS);
    }
}
