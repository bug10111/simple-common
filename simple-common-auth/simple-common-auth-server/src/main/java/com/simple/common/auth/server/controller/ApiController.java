package com.simple.common.auth.server.controller;

import com.simple.common.auth.server.common.service.user.LoginUserService;
import com.simple.common.core.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 框架默认api
 *
 * @author qty
 */
@Slf4j
@Tag(name = "框架默认api")
@RequestMapping("auth/api")
@RestController
public class ApiController {

    @Autowired
    private LoginUserService loginUserService;

    @GetMapping("user")
    @Operation(summary = "获取登录用户内省信息")
    public R<Map<String, String>> getSaveInfo() {
        return R.ok(loginUserService.getUserInformation());
    }

}

