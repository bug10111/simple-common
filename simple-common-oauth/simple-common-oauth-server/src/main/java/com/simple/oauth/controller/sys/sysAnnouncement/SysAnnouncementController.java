package com.simple.oauth.controller.sys.sysAnnouncement;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.auth.client.common.annotation.CsrfDefense;
import com.simple.common.auth.client.common.annotation.HasAuthority;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.mp.page.PageBase;
import com.simple.oauth.common.dto.sysAnnouncement.*;
import com.simple.oauth.common.service.sysAnnouncement.SysAnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统公告(sys_announcement)控制层
 *
 * @author qty
 */
@Slf4j
@Tag(name = "系统公告")
@RequestMapping("auth/sys-announcements")
@RestController
public class SysAnnouncementController {

    @Autowired
    private SysAnnouncementService sysAnnouncementService;

    @GetMapping("list")
    @Operation(summary = "分页查询系统公告")
    @HasAuthority("oauth")
    public R<IPage<SysAnnouncementPageResponse>> list(@ParameterObject FindAllSysAnnouncementRequest findAllRequest) {
        return R.ok(sysAnnouncementService.findAll(findAllRequest));
    }

    @PostMapping("create")
    @Operation(summary = "创建系统公告")
    @CsrfDefense
    @HasAuthority("oauth")
    public R<String> create(@RequestBody @Validated CreateSysAnnouncementRequest createRequest) {
        return R.ok(sysAnnouncementService.save(createRequest));
    }

    @GetMapping("current")
    @Operation(summary = "获取当前时间的公告")
    @HasAuthority("oauth")
    public R<IPage<SysAnnouncementPageResponse>> current(@ParameterObject PageBase pageBase) {
        return R.ok(sysAnnouncementService.findAll(pageBase));
    }

    @GetMapping("by-id/{id}")
    @Operation(summary = "查询单个系统公告")
    @HasAuthority("oauth")
    public R<SysAnnouncementInfoResponse> findOne(@PathVariable String id) {
        AssertUtils.notEmpty(id, "主键不能为空");
        return R.ok(sysAnnouncementService.findById(id));
    }

    @PutMapping("update/{id}")
    @Operation(summary = "更新单个系统公告")
    @HasAuthority("oauth")
    public R<Object> update(@PathVariable String id, @RequestBody @Validated UpdateSysAnnouncementRequest updateRequest) {
        AssertUtils.isTrue(updateRequest.getId().equals(id), "请求内容的ID与路径ID不同");
        sysAnnouncementService.updateById(updateRequest);
        return R.ok();
    }

    @DeleteMapping("delete")
    @Operation(summary = "删除系统公告")
    @HasAuthority("oauth")
    public R<Object> deleteByIds(@RequestBody List<String> ids) {
        AssertUtils.notEmpty(ids, "主键不能为空");
        sysAnnouncementService.deleteByIds(ids);
        return R.ok();
    }

}

