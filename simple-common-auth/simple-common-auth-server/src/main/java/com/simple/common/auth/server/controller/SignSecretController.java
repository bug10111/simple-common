package com.simple.common.auth.server.controller;

import com.simple.common.auth.client.common.manager.sign.SignManager;
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
 * 签名密钥控制器
 * <p>
 * 提供签名密钥的查询接口，供客户端远程拉取。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Tag(name = "签名密钥管理")
@RequestMapping("auth/api/sign")
@RestController
public class SignSecretController {

    @Autowired
    private SignManager signManager;

    /**
     * 获取当前签名密钥
     *
     * @return 签名密钥
     */
    @GetMapping("secret")
    @Operation(summary = "获取当前签名密钥")
    public R<Map<String, String>> getSignSecret() {

        // 生成新密钥（首次访问）
        String secret = signManager.generateSecret();
        signManager.addSecret(secret);
        
        Map<String, String> result = new HashMap<>();
        result.put("secret", secret);
        
        return R.ok(result);
    }
}
