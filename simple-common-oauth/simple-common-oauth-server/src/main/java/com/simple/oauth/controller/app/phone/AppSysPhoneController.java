package com.simple.oauth.controller.app.phone;

import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.client.common.annotation.HasAuthority;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.response.R;
import com.simple.oauth.common.dto.sysUser.BindingPhoneRequest;
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
 * 手机相关控制层
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Tag(name = "手机相关")
@RequestMapping("app")
@RestController
public class AppSysPhoneController {

    @Autowired
    private SysUserService sysUserService;

    @Operation(summary = "是否绑定了手机号")
    @PostMapping("has-phone")
    @HasAuthority("app")
    public R<Object> hasPhone() {
        SysUserInfoResponse byId = sysUserService.findById(LoginUserUtils.getUserTemporary().getUserId());
        return R.ok(ObjUtil.isNotEmpty(byId.getPhone()) ? byId.getPhone() : false);
    }

    @Operation(summary = "绑定手机号")
    @PostMapping("binding-phone")
    @HasAuthority("app")
    public R<Object> bindingPhone(@RequestBody @Validated BindingPhoneRequest request) {
        request.setSendType("CODE");
        sysUserService.bindingPhone(request);
        return R.ok();
    }

}

