package com.simple.oauth.controller.app.sysAnnouncement;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
import org.springframework.web.bind.annotation.*;

/**
 * 系统公告(sys_announcement)控制层
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Tag(name = "系统公告")
@RequestMapping("app/sys-announcements")
@RestController
public class AppSysAnnouncementController {

    @Autowired
    private SysAnnouncementService sysAnnouncementService;

    @GetMapping("current")
    @Operation(summary = "获取当前时间的公告")
    @HasAuthority("app")
    public R<IPage<SysAnnouncementPageResponse>> current(@ParameterObject PageBase pageBase) {
        return R.ok(sysAnnouncementService.findAll(pageBase));
    }

    @GetMapping("by-id/{id}")
    @Operation(summary = "查询单个系统公告")
    @HasAuthority("app")
    public R<SysAnnouncementInfoResponse> findOne(@PathVariable String id) {
        AssertUtils.notEmpty(id, "主键不能为空");
        return R.ok(sysAnnouncementService.findById(id));
    }
}

