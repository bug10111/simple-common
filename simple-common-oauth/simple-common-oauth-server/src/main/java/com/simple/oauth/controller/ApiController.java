package com.simple.oauth.controller;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.server.common.service.user.LoginUserService;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.sms.common.service.SmsService;
import com.simple.oauth.common.dto.api.ApiSysClientDetailsResponse;
import com.simple.oauth.common.dto.sms.SendSmsRequest;
import com.simple.oauth.common.dto.sysDictData.SysDictDatasResponse;
import com.simple.oauth.common.dto.sysRole.SysRoleInfoResponse;
import com.simple.oauth.common.dto.sysUser.CreateSysUserRequest;
import com.simple.oauth.common.dto.sysUser.RestSysUserRequest;
import com.simple.oauth.common.dto.sysUser.SysUserInfoResponse;
import com.simple.oauth.common.dto.sysUser.UpdateSysUserRequest;
import com.simple.oauth.common.service.sysClientDetails.SysClientDetailsService;
import com.simple.oauth.common.service.sysDictData.SysDictDataService;
import com.simple.oauth.common.service.sysRole.SysRoleService;
import com.simple.oauth.common.service.sysUser.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 对外部服务端提供的Api
 *
 * @author qty
 */
@Slf4j
@Tag(name = "对外部服务端提供的Api")
@RequestMapping("api")
@RestController
public class ApiController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private LoginUserService loginUserService;

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysClientDetailsService sysClientDetailsService;

    @Autowired
    private SysDictDataService sysDictDataService;

    @Autowired
    @Qualifier("aliSmsService")
    private SmsService smsService;

    @PostMapping("user")
    @Operation(summary = "创建用户")
    public R<String> create(@RequestBody @Validated CreateSysUserRequest createRequest) {
        return R.ok(sysUserService.save(createRequest));
    }

    @GetMapping("user/{id}")
    @Operation(summary = "查询单个用户")
    public R<SysUserInfoResponse> findOne(@PathVariable String id) {
        AssertUtils.notEmpty(id, "主键不能为空");
        return R.ok(sysUserService.findById(id));
    }

    @GetMapping("user/name/{name}")
    @Operation(summary = "查询单个用户")
    public R<Object> findOneByName(@PathVariable String name) {
        AssertUtils.notEmpty(name, "账号不能为空");
        return R.ok(sysUserService.findByName(name));
    }

    @GetMapping("role/{roleKey}")
    @Operation(summary = "根据角色获取用户列表")
    public R<Object> findOneByRoleKey(@PathVariable String roleKey) {
        AssertUtils.notEmpty(roleKey, "角色key不能为空");
        return R.ok(sysUserService.findOneByRoleKey(roleKey));
    }

    @GetMapping("role/id/{roleId}")
    @Operation(summary = "根据角色id获取用户列表")
    public R<Object> findOneByRoleId(@PathVariable String roleId) {
        AssertUtils.notEmpty(roleId, "角色key不能为空");
        return R.ok(sysUserService.findOneByRoleId(roleId));
    }

    @GetMapping("role/info/{id}")
    @Operation(summary = "查询单个角色信息")
    public R<SysRoleInfoResponse> findRoleById(@PathVariable String id) {
        Assert.notNull(id, "主键不能为空");
        return R.ok(sysRoleService.findById(id));
    }

    @GetMapping("user")
    @Operation(summary = "获取登录用户内省信息")
    public R<Map<String, String>> getSaveInfo() {
        return R.ok(loginUserService.getUserInformation());
    }

    @PostMapping("user/{id}")
    @Operation(summary = "更新单个用户")
    public R<Object> update(@PathVariable String id, @RequestBody @Validated UpdateSysUserRequest updateRequest) {
        AssertUtils.isTrue(updateRequest.getId().equals(id), "请求内容的ID与路径ID不同");
        sysUserService.updateById(updateRequest);
        return R.ok();
    }

    @PostMapping("user/reset/{id}")
    @Operation(summary = "重置密码")
    public R<Object> reset(@PathVariable String id, @RequestBody @Validated RestSysUserRequest updateRequest) {
        AssertUtils.isTrue(updateRequest.getId().equals(id), "请求内容的ID与路径ID不同");
        sysUserService.resetPwd(updateRequest);
        return R.ok();
    }

    @DeleteMapping("user")
    @Operation(summary = "删除用户")
    public R<Object> deleteByIds(@RequestBody List<String> ids) {
        AssertUtils.isTrue(ObjUtil.isNotEmpty(ids), "主键不能为空");
        sysUserService.deleteByIds(ids);
        return R.ok();
    }

    @Operation(summary = "发送短信")
    @PostMapping("sms/send")
    public R<Object> handler(@RequestBody @Validated SendSmsRequest sendSmsRequest) {
        smsService.sendCode(sendSmsRequest.getPhone(), sendSmsRequest.getCode(), sendSmsRequest.getSendType());
        return R.ok();
    }

    @Operation(summary = "校验短信验证码")
    @PostMapping("sms/check-sms")
    public R<Object> checkSms(@RequestBody @Validated SendSmsRequest sendSmsRequest) {
        smsService.checkSms(sendSmsRequest.getPhone(), sendSmsRequest.getCode(), sendSmsRequest.getSendType());
        return R.ok();
    }

    @GetMapping("client/list/{server}")
    @Operation(summary = "某服务客户端列表")
    public R<List<ApiSysClientDetailsResponse>> list(@PathVariable String server) {
        AssertUtils.notEmpty(server, "客户端不能为空");
        return R.ok(sysClientDetailsService.list(server));
    }

    @PostMapping("labelList")
    @Operation(summary = "获取多个类型的字典数据")
    public R<Map<String, List<SysDictDatasResponse>>> labelList(@RequestBody List<String> dictValues) {
        return R.ok(sysDictDataService.labelList(dictValues));
    }

}

