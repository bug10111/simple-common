package com.simple.oauth.controller.pub;

import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.auth.server.common.service.login.LoginService;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.DateUtils;
import com.simple.common.redis.annotation.CurrentLimiting;
import com.simple.common.redis.common.enums.CurrentLimitingRulesEnum;
import com.simple.oauth.common.dto.login.RefreshDto;
import com.simple.oauth.common.dto.sysUser.UpdatePwdRequest;
import com.simple.oauth.common.dto.sysUser.UpdateSysUserRequest;
import com.simple.oauth.common.dto.wxLogin.WeChatLoginRequest;
import com.simple.oauth.common.entity.login.PwdLoginRequest;
import com.simple.oauth.common.enums.LoginType;
import com.simple.oauth.common.service.sysUser.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Tag(name = "授权相关")
@RequestMapping("auth")
@RestController
public class LoginController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private SysUserService sysUserService;

    @Operation(summary = "登录")
    @PostMapping("login")
    public R<Map<String, String>> login(@RequestBody @Validated PwdLoginRequest request) {
        return R.ok(loginService.login(request, LoginType.PWD_LOGIN));
    }

    @Operation(summary = "微信登录")
    @PostMapping("wx-login")
    public R<Map<String, String>> wxLogin(@RequestBody @Validated WeChatLoginRequest request) {
        return R.ok(loginService.login(request, LoginType.WX_LOGIN));
    }

    @Operation(summary = "获取当前时间")
    @CurrentLimiting(key = CurrentLimitingRulesEnum.IP, time = 1, sum = 5)
    @GetMapping("getTime")
    public R<String> getTime() {
        return R.ok(DateUtils.getNetworkDate());
    }

    @Operation(summary = "刷新登录")
    @PostMapping("refresh")
    public R<Map<String, String>> refresh(@RequestBody RefreshDto refresh) {
        return R.ok(loginService.refresh(refresh.getRefresh()));
    }

    @Operation(summary = "退出当前登录用户")
    @PostMapping("loginOut")
    public R<Object> loginOut() {
        loginService.logout();
        return R.ok();
    }

    @Operation(summary = "指定用户退出登录")
    @PostMapping("loginOut/{userId}")
    public R<Object> loginOut(@PathVariable String userId) {
        loginService.logout(userId);
        return R.ok();
    }

    @PutMapping("update-pwd")
    @Operation(summary = "修改密码")
    public R<Object> updatePwd(@RequestBody @Validated UpdatePwdRequest request) {
        if (ObjUtil.isEmpty(request.getId())) {
            request.setId(LoginUserUtils.getUserTemporary().getUserId());
        }
        sysUserService.updateById(new UpdateSysUserRequest().setId(request.getId()).setPasswordNew(request.getNewPwd()).setPasswordOld(request.getOldPwd()));
        return R.ok();
    }
}

