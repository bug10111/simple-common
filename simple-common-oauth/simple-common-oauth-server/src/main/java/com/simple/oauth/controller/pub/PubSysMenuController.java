package com.simple.oauth.controller.pub;

import com.simple.common.auth.client.common.entity.login.UserTemporary;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.response.R;
import com.simple.oauth.common.dto.sysMenu.SysMenuPageResponse;
import com.simple.oauth.common.service.sysMenu.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 菜单权限(sys_menu)控制层
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Tag(name = "菜单权限")
@RequestMapping("auth/sys-menus")
@RestController
public class PubSysMenuController {

    @Autowired
    private SysMenuService sysMenuService;

    @GetMapping("login")
    @Operation(summary = "获取登录用户客户端菜单")
    public R<Set<SysMenuPageResponse>> findAllByLoginUserByClient() {
        UserTemporary userTemporary = LoginUserUtils.getUserTemporary();
        return R.ok(sysMenuService.findAllByLoginUser(userTemporary.getLoginRole(), userTemporary.getUserId(), userTemporary.getClientId()));
    }

    @GetMapping("login/all")
    @Operation(summary = "获取登录用户所有菜单")
    public R<Set<SysMenuPageResponse>> findAllByLoginUserAll() {
        UserTemporary userTemporary = LoginUserUtils.getUserTemporary();
        return R.ok(sysMenuService.findAllByLoginUser(userTemporary.getLoginRole(), userTemporary.getUserId(), null));
    }
}

