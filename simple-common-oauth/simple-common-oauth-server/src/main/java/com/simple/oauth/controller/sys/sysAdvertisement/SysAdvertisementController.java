package com.simple.oauth.controller.sys.sysAdvertisement;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.auth.client.common.annotation.CsrfDefense;
import com.simple.common.auth.client.common.annotation.HasAuthority;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.oauth.common.dto.sysAdvertisement.*;
import com.simple.oauth.common.service.sysAdvertisement.SysAdvertisementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 广告表(sys_advertisement)控制层
 *
 * @author qty
 */
@Slf4j
@Tag(name = "广告表")
@RequestMapping("auth/sys-advertisements")
@RestController
public class SysAdvertisementController {

    @Autowired
    private SysAdvertisementService sysAdvertisementService;

    @GetMapping("list")
    @Operation(summary = "分页查询广告表")
    @HasAuthority("oauth")
    public R<IPage<SysAdvertisementPageResponse>> list(@ParameterObject FindAllSysAdvertisementRequest findAllRequest) {
        return R.ok(sysAdvertisementService.findAll(findAllRequest));
    }

    @PostMapping("create")
    @CsrfDefense
    @Operation(summary = "创建广告表")
    @HasAuthority("oauth")
    public R<String> create(@RequestBody @Validated CreateSysAdvertisementRequest createRequest) {
        return R.ok(sysAdvertisementService.save(createRequest));
    }

    @GetMapping("by-id/{id}")
    @Operation(summary = "查询单个广告表")
    @HasAuthority("oauth")
    public R<SysAdvertisementInfoResponse> findOne(@PathVariable String id) {
        AssertUtils.notEmpty(id, "主键不能为空");
        return R.ok(sysAdvertisementService.findById(id));
    }

    @PutMapping("update/{id}")
    @Operation(summary = "更新单个广告表")
    @HasAuthority("oauth")
    public R<Object> update(@PathVariable String id, @RequestBody @Validated UpdateSysAdvertisementRequest updateRequest) {
        AssertUtils.isTrue(updateRequest.getId().equals(id), "请求内容的ID与路径ID不同");
        sysAdvertisementService.updateById(updateRequest);
        return R.ok();
    }

    @DeleteMapping("delete")
    @Transactional
    @Operation(summary = "删除广告表")
    @HasAuthority("oauth")
    public R<Object> deleteByIds(@RequestBody List<String> ids) {
        AssertUtils.notEmpty(ids, "主键不能为空");
        sysAdvertisementService.deleteByIds(ids);
        return R.ok();
    }

    @PostMapping("disable/{id}")
    @Operation(summary = "禁用")
    @HasAuthority("oauth")
    public R<Object> disable(@PathVariable String id) {
        sysAdvertisementService.disable(id);
        return R.ok();
    }

    @PostMapping("enable/{id}")
    @Operation(summary = "启用")
    @HasAuthority("oauth")
    public R<Object> enable(@PathVariable String id) {
        sysAdvertisementService.enable(id);
        return R.ok();
    }

}

