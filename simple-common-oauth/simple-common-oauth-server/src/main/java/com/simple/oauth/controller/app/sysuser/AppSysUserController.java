package com.simple.oauth.controller.app.sysuser;

import cn.hutool.core.util.StrUtil;
import com.simple.common.auth.client.common.annotation.HasAuthority;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.response.R;
import com.simple.oauth.common.dto.sysUser.BindingAccountRequest;
import com.simple.oauth.common.dto.sysUser.SysUserInfoResponse;
import com.simple.oauth.common.service.sysUser.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户相关控制层
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Tag(name = "用户相关")
@RequestMapping("app")
@RestController
public class AppSysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Operation(summary = "是否绑定了账号")
    @PostMapping("has-account")
    @HasAuthority("app")
    public R<Object> hasAccount() {
        SysUserInfoResponse byId = sysUserService.findById(LoginUserUtils.getUserTemporary().getUserId());
        return R.ok(!StrUtil.containsAny(byId.getUsername(), "txc", "admin") ? byId.getUsername() : false);
    }

    @Operation(summary = "绑定账号")
    @PostMapping("binding-account")
    @HasAuthority("app")
    public R<Object> bindingAccount(@RequestBody @Validated BindingAccountRequest request) {
        sysUserService.bindingAccount(request);
        return R.ok();
    }

    @Operation(summary = "解除账号绑定")
    @PostMapping("disarm-account")
    @HasAuthority("app")
    public R<Object> disarmAccount() {
        sysUserService.disarmAccount();
        return R.ok();
    }

}

