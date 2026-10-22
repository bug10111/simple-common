package com.simple.common.auth.client.controller;

import com.simple.common.auth.client.common.properties.CsrfProperties;
import com.simple.common.auth.client.common.service.CsrfService;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
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
 *
 * @author qty
 */
@Slf4j
@Tag(name = "CSRF")
@RequestMapping("csrf")
@RestController
public class CsrfTokenController {

    /**
     * Token 生成专用 CSPRNG：SecureRandom 线程安全，静态复用（每请求 new 有熵源阻塞与开销问题）
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private CsrfProperties csrfProperties;

    @Autowired
    private CsrfService csrfService;

    /**
     * 生成 CSRF Token（凭证禁缓存下发头）。
     * <p>
     * 注意：本接口为 GET 且带状态（覆盖缓存），跨站 GET 可被恶意页面反复调用刷新 Token，
     * 属可接受形态（须登录态 + 服务端 session 绑定；刷新只会覆盖同键缓存，不放大权限），
     * 前端契约已定型为 GET，改 POST 属破坏性变更，保持现状。
     * </p>
     *
     * @param path 目标接口路径（不允许为空：Token 与路径强绑定，缺省将写入错误缓存键导致校验恒失败）
     */
    @Operation(summary = "获取CSRF Token", parameters = { @Parameter(name = "path", description = "目标接口path") })
    @GetMapping("/generate")
    public R<Object> generateCsrfToken(String path) {
        AssertUtils.notEmpty(path, "请求路径不能为空");

        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String csrfToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        // getUserTemporary 不返回 null：未登录为空对象，userId 判空即登录校验（fail-closed，不再向匿名请求发放 token）
        String userId = LoginUserUtils.getUserTemporary().getUserId();
        AssertUtils.notEmpty(userId, "用户未登录，无法生成 CSRF Token");

        csrfService.saveToken(userId, path, csrfToken);

        HttpServletResponse response = HttpServletUtils.getResponse();
        response.addHeader(csrfProperties.getCsrfHeader(), csrfToken);
        // 追加而非覆盖：不破坏全局 CORS 已暴露的其它响应头
        response.addHeader("Access-Control-Expose-Headers", csrfProperties.getCsrfHeader());
        // 凭证类响应禁缓存：防止浏览器/共享代理存储并重放有效 token
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        return R.ok();
    }
}
