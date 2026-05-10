package com.simple.common.auth.server.controller;

import com.simple.common.auth.server.common.manager.secret.UnifiedSecretManager;
import com.simple.common.core.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 统一密钥控制器
 * <p>
 * 提供根据项目编码获取JWT和SIGN双密钥的接口，供客户端远程拉取。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Tag(name = "统一密钥管理")
@RequestMapping("auth/api/secrets")
@RestController
public class UnifiedSecretController {

    @Autowired
    private UnifiedSecretManager unifiedSecretManager;

    /**
     * 根据项目编码获取密钥
     *
     * @param projectCode 项目编码（spring.application.name）
     * @return 包含jwt和sign密钥的Map
     */
    @GetMapping
    @Operation(summary = "根据项目编码获取密钥")
    public R<Map<String, String>> getSecrets(@Parameter(description = "项目编码（spring.application.name）", required = true) @RequestParam String projectCode) {

        log.debug("项目 [{}] 请求获取密钥", projectCode);

        // 获取双密钥
        Map<String, String> secrets = unifiedSecretManager.getSecrets(projectCode);

        return R.ok(secrets);
    }
}
