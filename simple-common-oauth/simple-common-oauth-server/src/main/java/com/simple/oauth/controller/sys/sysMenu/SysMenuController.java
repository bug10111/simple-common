package com.simple.oauth.controller.sys.sysMenu;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.client.common.annotation.CsrfDefense;
import com.simple.common.auth.client.common.annotation.HasAuthority;
import com.simple.common.auth.client.common.entity.login.UserTemporary;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.oauth.common.dto.sysMenu.CreateSysMenuRequest;
import com.simple.oauth.common.dto.sysMenu.SysMenuInfoResponse;
import com.simple.oauth.common.dto.sysMenu.SysMenuPageResponse;
import com.simple.oauth.common.dto.sysMenu.UpdateSysMenuRequest;
import com.simple.oauth.common.service.sysMenu.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单权限(sys_menu)控制层
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Tag(name = "菜单权限")
@RequestMapping("auth/sys-menus")
@RestController
public class SysMenuController {

    @Autowired
    private SysMenuService sysMenuService;

    @GetMapping("menus/{client}")
    @Operation(summary = "获取客户端所有菜单")
    @HasAuthority("oauth")
    public R<List<Tree<String>>> list(@PathVariable String client) {
        AssertUtils.notEmpty(client, "客户端ID不能为空");
        return R.ok(sysMenuService.findAll(client));
    }

    @GetMapping("menus")
    @Operation(summary = "获取所有菜单")
    @HasAuthority("oauth")
    public R<List<Tree<String>>> menus() {
        return R.ok(sysMenuService.findAll(null));
    }

    @PostMapping
    @Operation(summary = "创建菜单权限")
    @HasAuthority("oauth")
    @CsrfDefense
    public R<String> create(@RequestBody @Validated CreateSysMenuRequest createRequest) {
        return R.ok(sysMenuService.save(createRequest));
    }

    @GetMapping("{id}")
    @Operation(summary = "查询单个菜单权限")
    @HasAuthority("oauth")
    public R<SysMenuInfoResponse> findOne(@PathVariable String id) {
        Assert.notNull(id, "主键不能为空");
        return R.ok(sysMenuService.findById(id));
    }

    @PutMapping("{id}")
    @Operation(summary = "更新单个菜单权限")
    @HasAuthority("oauth")
    public R<Object> update(@PathVariable String id, @RequestBody @Validated UpdateSysMenuRequest updateRequest) {
        Assert.isTrue(updateRequest.getId().equals(id), "请求内容的ID与路径ID不同");
        sysMenuService.updateById(updateRequest);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除菜单权限")
    @HasAuthority("oauth")
    public R<Object> deleteByIds(@RequestBody List<String> ids) {
        Assert.isTrue(ObjUtil.isNotEmpty(ids), "主键不能为空");
        sysMenuService.deleteByIds(ids);
        return R.ok();
    }
}

