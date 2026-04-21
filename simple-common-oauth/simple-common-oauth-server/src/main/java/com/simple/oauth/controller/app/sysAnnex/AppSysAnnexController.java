package com.simple.oauth.controller.app.sysAnnex;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.annex.common.enums.ShareType;
import com.simple.common.auth.client.common.annotation.HasAuthority;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.oauth.common.dto.sysAnnex.AnnexListResponse;
import com.simple.oauth.common.dto.sysAnnex.FindAllSysAnnexRequest;
import com.simple.oauth.common.dto.sysAnnex.SysAnnexPageResponse;
import com.simple.oauth.common.dto.sysAnnex.UpdateSysAnnexRequest;
import com.simple.oauth.common.service.sysAnnex.SysAnnexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 附件(sys_annex)控制层
 *
 * @author qty
 */
@Slf4j
@Tag(name = "附件")
@RequestMapping("app/sys-annexs")
@RestController
public class AppSysAnnexController {

    @Autowired
    private SysAnnexService sysAnnexService;

    @GetMapping("list")
    @Operation(summary = "分页查询附件")
    @HasAuthority("oauth")
    public R<IPage<SysAnnexPageResponse>> list(@ParameterObject FindAllSysAnnexRequest findAllRequest) {
        return R.ok(sysAnnexService.findAll(findAllRequest));
    }

    @Operation(summary = "文件上传")
    @PostMapping("upload")
    @HasAuthority("app")
    public R<Map<String, String>> upload(@RequestParam("filter") MultipartFile filter, ShareType shareType) {
        return R.ok(sysAnnexService.save(filter, shareType));
    }

    @Operation(summary = "获取文件URL")
    @GetMapping("by-id/{id}")
    public R<Object> currentLimiting(@PathVariable String id) {
        return R.ok(sysAnnexService.get(id));
    }

    @Operation(summary = "获取文件URL集合")
    @PostMapping("annex-list")
    @HasAuthority("app")
    public R<List<AnnexListResponse>> annexList(@RequestBody List<String> ids) {
        AssertUtils.notEmpty(ids, "主键不能为空");
        return R.ok(sysAnnexService.get(ids));
    }
}

