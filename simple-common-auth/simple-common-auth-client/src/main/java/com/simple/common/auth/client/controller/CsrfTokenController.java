package com.simple.common.auth.client.controller;

import cn.hutool.core.util.IdUtil;
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
 * Created with IntelliJ IDEA
 * Description: 用于获取CSRF Token的Controller
 *
 * @author qty
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

    @Operation(summary = "获取CSRF Token，建议在页面初始化前调用，调用成功后，渲染到表单标签里面，例如按钮的某个属性中。再调用目标接口携带参数", parameters = {
                    @Parameter(name = "path", description = "目标接口path") })
    @GetMapping("/generate")
    public R<Object> generateCsrfToken(String path) {
        SecureRandom random = new SecureRandom();
        byte[] token = new byte[32];
        random.nextBytes(token);
        String csrfToken = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
        String userId = LoginUserUtils.getUserTemporary().getUserId();

        csrfService.saveToken(userId, path, csrfToken);

        HttpServletResponse response = HttpServletUtils.getResponse();
        response.addHeader(csrfProperties.getCsrfTokenHeader(), csrfToken);
        response.setHeader("Access-Control-Expose-Headers", csrfProperties.getCsrfTokenHeader());
        return R.ok();
    }
}

