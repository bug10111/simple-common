package com.simple.oauth.controller.app.sysAdvertisement;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.auth.client.common.annotation.CsrfDefense;
import com.simple.common.auth.client.common.annotation.HasAuthority;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.mp.common.enums.Status;
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
@RequestMapping("app/sys-advertisements")
@RestController
public class AppSysAdvertisementController {

    @Autowired
    private SysAdvertisementService sysAdvertisementService;

    @GetMapping("list")
    @Operation(summary = "分页查询广告表")
    public R<IPage<SysAdvertisementPageResponse>> list(@ParameterObject FindAllSysAdvertisementRequest findAllRequest) {
        findAllRequest.setStatus(Status.ON);
        return R.ok(sysAdvertisementService.findAll(findAllRequest));
    }

    @GetMapping("by-id/{id}")
    @Operation(summary = "查询单个广告表")
    public R<SysAdvertisementInfoResponse> findOne(@PathVariable String id) {
        AssertUtils.notEmpty(id, "主键不能为空");
        return R.ok(sysAdvertisementService.findById(id));
    }
}

