package com.simple.common.auth.client.common.annotation.aspect;

import com.simple.common.auth.client.common.annotation.CsrfDefense;
import com.simple.common.auth.client.common.entity.login.UserTemporary;
import com.simple.common.auth.client.common.properties.CsrfProperties;
import com.simple.common.auth.client.common.service.CsrfService;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.HttpServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * CSRF 防御切面。
 * <p>
 * Token 与「当前请求 URI」强绑定：校验路径一律取自本次请求的 requestURI，
 * 不信任 ThreadLocal 中的 path（其可能未被设置或残留上一请求的值，造成跨端点 Token 误用）。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Aspect
@Component
public class CsrfDefenseAspect {

    @Autowired
    private CsrfProperties csrfProperties;

    @Autowired
    private CsrfService csrfService;

    @Before("@annotation(com.simple.common.auth.client.common.annotation.CsrfDefense)")
    public void before(JoinPoint joinPoint) {
        if (csrfProperties.isCsrfDefense()) {
            // 获取注解实例
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            CsrfDefense csrfDefense = method.getAnnotation(CsrfDefense.class);

            // 获取当前登录用户（getUserTemporary 不返回 null：未登录返回空对象，以下 userId 判空即登录校验）
            UserTemporary userTemporary = LoginUserUtils.getUserTemporary();
            String userId = userTemporary.getUserId();
            // CSRF 校验前提是已登录：未登录直接拒绝（fail-closed），不生成/不消费任何 Token
            AssertUtils.notEmpty(userId, "用户ID为空，无法进行 CSRF 校验");

            HttpServletRequest request = HttpServletUtils.getRequest();
            // 路径以本次请求为准（ThreadLocal path 可能缺失/残留上一请求值，导致跨端点重放）
            String path = request.getRequestURI();
            AssertUtils.notEmpty(path, "请求路径不能为空");

            String token = request.getHeader(csrfProperties.getCsrfHeader());
            AssertUtils.notEmpty(token, "请求失败", "用户[{}]==>[{}] CSRF token不存在", userId, path);

            // 传入 consume 参数
            csrfService.checkToken(userId, path, token, csrfDefense.consume());
        }
    }
}
