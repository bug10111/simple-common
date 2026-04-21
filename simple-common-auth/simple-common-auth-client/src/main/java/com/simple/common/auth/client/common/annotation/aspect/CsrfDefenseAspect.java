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

            // 获取当前登录用户
            UserTemporary userTemporary = LoginUserUtils.getUserTemporary();
            AssertUtils.notNull(userTemporary, "用户未登录，无法进行 CSRF 校验");
            String userId = userTemporary.getUserId();
            String path = userTemporary.getPath();
            AssertUtils.notEmpty(userId, "用户ID为空，无法进行 CSRF 校验");
            AssertUtils.notEmpty(path, "请求路径不能为空");

            HttpServletRequest request = HttpServletUtils.getRequest();
            String token = request.getHeader(csrfProperties.getCsrfHeader());
            AssertUtils.notEmpty(token, "请求失败", "用户[{}]==>[{}] CSRF token不存在", userId, path);
            
            // 传入 consume 参数
            csrfService.checkToken(userId, path, token, csrfDefense.consume());
        }
    }
}