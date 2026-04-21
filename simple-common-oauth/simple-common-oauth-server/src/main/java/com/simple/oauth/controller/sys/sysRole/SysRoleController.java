package com.simple.oauth.controller.sys.sysRole;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.auth.client.common.annotation.CsrfDefense;
import com.simple.common.auth.client.common.annotation.HasAuthority;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.response.R;
import com.simple.oauth.common.dto.sysRole.*;
import com.simple.oauth.common.dto.sysUser.BindingRoleRequest;
import com.simple.oauth.common.manager.role.RoleAuthCacheManager;
import com.simple.oauth.common.service.sysMenu.SysMenuService;
import com.simple.oauth.common.service.sysRole.SysRoleService;
import com.simple.oauth.common.service.sysUser.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色信息(sys_role)控制层
 *
 * @author qty
 */
@Slf4j
@Tag(name = "角色信息")
@RequestMapping("auth/sys-roles")
@RestController
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysMenuService sysMenuService;

    @Autowired
    private RoleAuthCacheManager roleAuthCacheManager;

    @GetMapping
    @Operation(summary = "分页查询角色信息")
    @HasAuthority("oauth")
    public R<IPage<SysRolePageResponse>> list(@ParameterObject FindAllSysRoleRequest findAllRequest) {
        return R.ok(sysRoleService.findAll(findAllRequest));
    }

    @GetMapping("list/user")
    @Operation(summary = "获取当前用户拥有的角色")
    @HasAuthority("oauth")
    public R<List<SysRolePageResponse>> listbyUser(@ParameterObject FindAllSysRoleRequest findAllRequest) {
        findAllRequest.setUserId(LoginUserUtils.getUserTemporary().getUserId());
        return R.ok(sysRoleService.listbyUser(findAllRequest));
    }

    @GetMapping({ "{roleId}/menu" })
    @Operation(summary = "根据角色获取权限信息")
    @HasAuthority("oauth")
    public R<List<Tree<String>>> roleMenuList(@PathVariable String roleId) {
        Assert.notNull(roleId, "角色ID不能为空");
        return R.ok(sysMenuService.findAllByRoleId(roleId, null));
    }

    @PostMapping
    @Operation(summary = "创建角色信息")
    @CsrfDefense
    @HasAuthority("oauth")
    public R<String> create(@RequestBody @Validated CreateSysRoleRequest createRequest) {
        return R.ok(sysRoleService.save(createRequest));
    }

    @PostMapping("{userId}/role")
    @Operation(summary = "分配角色")
    @HasAuthority("oauth")
    public R<Object> bindingRole(@PathVariable String userId, @RequestBody @Validated List<BindingRoleRequest> request) {
        sysUserService.bindingRole(userId, request);
        return R.ok();
    }

    @GetMapping("{userId}/role")
    @Operation(summary = "获取用户角色列表")
    @HasAuthority("oauth")
    public R<List<SysRoleInfoResponse>> getRole(@PathVariable String userId) {
        return R.ok(sysRoleService.getRole(userId));
    }

    @GetMapping("{id}")
    @Operation(summary = "查询单个角色信息")
    @HasAuthority("oauth")
    public R<SysRoleInfoResponse> findOne(@PathVariable String id) {
        Assert.notNull(id, "主键不能为空");
        return R.ok(sysRoleService.findById(id));
    }

    @PutMapping("{id}")
    @Operation(summary = "更新单个角色信息")
    @HasAuthority("oauth")
    public R<Object> update(@PathVariable String id, @RequestBody @Validated UpdateSysRoleRequest updateRequest) {
        Assert.isTrue(updateRequest.getId().equals(id), "请求内容的ID与路径ID不同");
        sysRoleService.updateById(updateRequest);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除角色信息")
    @HasAuthority("oauth")
    public R<Object> deleteByIds(@RequestBody List<String> ids) {
        Assert.isTrue(ObjUtil.isNotEmpty(ids), "主键不能为空");
        sysRoleService.deleteByIds(ids);
        return R.ok();
    }

    @GetMapping("update-cache")
    @Operation(summary = "更新缓存（直接修改了菜单，admin会自动更新，其他角色需要手动刷新）")
    @HasAuthority("oauth")
    public R<Object> cache() {
        roleAuthCacheManager.update();
        return R.ok();
    }
}

