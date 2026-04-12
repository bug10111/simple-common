package com.simple.common.auth.client.controller;

import com.simple.common.auth.client.common.properties.CsrfProperties;
import com.simple.common.auth.client.common.service.CsrfService;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.HttpServletUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * CSRF Token 生成控制器。
 * <p>
 * 修复：将 csrfService.createToken(...) 改为 saveToken(...)，与接口定义一致。
 *
 * @author qty (修复版本)
 */
@Slf4j
@Tag(name = "CSRF")
@RequestMapping("csrf")
@RestController
public class CsrfTokenController {

    @Autowired
    private CsrfProperties csrfProperties;

    @Autowired
    private CsrfService csrfService;

    @Operation(summary = "获取CSRF Token", parameters = {
                    @Parameter(name = "path", description = "目标接口path")
    })
    @GetMapping("/generate")
    public R<Object> generateCsrfToken(String path) {
        SecureRandom random = new SecureRandom();
        byte[] token = new byte[32];
        random.nextBytes(token);
        String csrfToken = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
        String userId = LoginUserUtils.getUserTemporary().getUserId();

        // 修正：使用 saveToken 方法（与 CsrfService 接口一致）
        csrfService.saveToken(userId, path, csrfToken);

        HttpServletResponse response = HttpServletUtils.getResponse();
        response.addHeader(csrfProperties.getCsrfHeader(), csrfToken);
        response.setHeader("Access-Control-Expose-Headers", csrfProperties.getCsrfHeader());
        return R.ok();
    }
}