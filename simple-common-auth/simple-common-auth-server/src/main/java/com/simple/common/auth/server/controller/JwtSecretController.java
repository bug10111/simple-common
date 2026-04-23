package com.simple.common.auth.server.controller;

import com.simple.common.auth.client.common.manager.token.TokenManager;
import com.simple.common.core.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * JWT密钥控制器
 * <p>
 * 提供JWT密钥的查询接口，供客户端远程拉取。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Tag(name = "JWT密钥管理")
@RequestMapping("auth/api/jwt")
@RestController
public class JwtSecretController {

    @Autowired
    private TokenManager tokenManager;

    /**
     * 获取当前JWT密钥
     *
     * @return JWT密钥
     */
    @GetMapping("secret")
    @Operation(summary = "获取当前JWT密钥")
    public R<Map<String, String>> getJwtSecret() {
        // 生成新密钥（首次访问）
        String secret = tokenManager.generateSecret();
        tokenManager.addSecret(secret);
        
        Map<String, String> result = new HashMap<>();
        result.put("secret", secret);
        
        return R.ok(result);
    }
}
