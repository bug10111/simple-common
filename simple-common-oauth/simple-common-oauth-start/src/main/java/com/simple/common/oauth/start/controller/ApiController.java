//package com.simple.common.oauth.start.controller;
//
//import cn.hutool.core.util.ObjUtil;
//import com.simple.common.core.response.R;
//import com.simple.common.core.utils.AssertUtils;
//import com.simple.common.oauth.start.common.entity.CreateSysUserRequest;
//import com.simple.common.oauth.start.common.entity.SysUserInfoResponse;
//import com.simple.common.oauth.start.common.entity.UpdateSysUserRequest;
//import com.simple.common.oauth.start.common.service.UserService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
///**
// * 对外部服务端提供的Api
// *
// * @author qty
// */
//@Slf4j
//@Tag(name = "用户")
//@RequestMapping("api")
//@RestController
//public class ApiController {
//
//    @Autowired
//    private UserService UserService;
//
//    @PostMapping("user")
//    @Operation(summary = "创建用户")
//    public R<String> create(@RequestBody CreateSysUserRequest createRequest) {
//        return R.ok(UserService.create(createRequest));
//    }
//
//    @GetMapping("user/{id}")
//    @Operation(summary = "查询单个用户")
//    public R<SysUserInfoResponse> findOne(@PathVariable String id) {
//        AssertUtils.notEmpty(id, "主键不能为空");
//        return R.ok(UserService.findById(id));
//    }
//
//    @PostMapping("user/{id}")
//    @Operation(summary = "更新单个用户")
//    public R<Object> update(@PathVariable String id, @RequestBody UpdateSysUserRequest updateRequest) {
//        AssertUtils.isTrue(updateRequest.getId().equals(id), "请求内容的ID与路径ID不同");
//        UserService.update(updateRequest);
//        return R.ok();
//    }
//
//    @DeleteMapping("user")
//    @Operation(summary = "删除用户")
//    public R<Object> deleteByIds(@RequestBody List<String> ids) {
//        AssertUtils.isTrue(ObjUtil.isNotEmpty(ids), "主键不能为空");
//        UserService.delete(ids);
//        return R.ok();
//    }
//}
//
