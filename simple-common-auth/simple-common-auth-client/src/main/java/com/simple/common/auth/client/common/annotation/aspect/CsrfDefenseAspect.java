package com.simple.common.auth.client.common.annotation.aspect;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.properties.CsrfProperties;
import com.simple.common.auth.client.common.service.CsrfService;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.common.service.lock.LockService;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.HttpServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Aspect
@Component
public class CsrfDefenseAspect {

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Autowired
    private CsrfProperties csrfProperties;

    @Autowired
    private CsrfService csrfService;

    @Autowired
    private LockService lockService;

    @Before("@annotation(com.simple.common.auth.client.common.annotation.CsrfDefense)")
    public void before() {
        if (csrfProperties.isCsrfDefense()) {

            String userId = LoginUserUtils.getUserTemporary().getUserId();
            String path = LoginUserUtils.getUserTemporary().getPath();
            HttpServletRequest request = HttpServletUtils.getRequest();

            String token = request.getHeader(csrfProperties.getCsrfTokenHeader());
            AssertUtils.notEmpty(token, "请求失败", "用户[{}]==>[{}] CSRF token不存在", userId, path);
            csrfService.checkToken(userId, path, token);
        }
    }
}
