package com.simple.common.auth.server.controller;

import com.simple.common.auth.server.common.service.user.LoginUserService;
import com.simple.common.core.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 框架默认api。
 *
 * @author qty
 */
@Tag(name = "框架默认api")
@RequestMapping("auth/api")
@RestController
public class ApiController {

    @Autowired
    private LoginUserService loginUserService;

    /**
     * 获取当前登录用户的内省信息（用户信息 + 关联 token 集合 + 角色-权限）。
     * <p>
     * 访问边界：本端点由认证拦截器（AuthHandlerInterceptor）构建的登录上下文供数，
     * 上下文缺失时服务层快速失败（登录失效），不会回退到空键查询；
     * 生产部署仍要求网关/拦截器链确保 /auth/api/** 仅对携带有效令牌的请求开放。
     * </p>
     *
     * @return 内省信息（userInfo/userToken/userAuth 三个 JSON 串）
     */
    @GetMapping("user")
    @Operation(summary = "获取登录用户内省信息（包含权限）")
    public R<Map<String, String>> getCurrentUserInfo() {
        return R.ok(loginUserService.getUserInformation());
    }

}
