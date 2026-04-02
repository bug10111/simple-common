package com.simple.oauth.controller.sys.sysAnnex;

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
 * @author 兄台丶请冷静
 */
@Slf4j
@Tag(name = "附件")
@RequestMapping("auth/sys-annexs")
@RestController
public class SysAnnexController {

    @Autowired
    private SysAnnexService sysAnnexService;

    @GetMapping("list")
    @Operation(summary = "分页查询附件")
    @HasAuthority("oauth")
    public R<IPage<SysAnnexPageResponse>> list(@ParameterObject FindAllSysAnnexRequest findAllRequest) {
        return R.ok(sysAnnexService.findAll(findAllRequest));
    }

    @PutMapping("update/{id}")
    @Operation(summary = "更新单个附件")
    @HasAuthority("oauth")
    public R<Object> update(@PathVariable String id, @RequestBody @Validated UpdateSysAnnexRequest updateRequest) {
        AssertUtils.isTrue(updateRequest.getId().equals(id), "请求内容的ID与路径ID不同");
        sysAnnexService.updateById(updateRequest);
        return R.ok();
    }

    @Operation(summary = "文件上传")
    @PostMapping("upload")
    @HasAuthority("oauth")
    public R<Map<String, String>> upload(MultipartFile filter, ShareType shareType) {
        return R.ok(sysAnnexService.save(filter, shareType));
    }

    @Operation(summary = "获取文件URL")
    @GetMapping("by-id/{id}")
    @HasAuthority("oauth")
    public R<Object> currentLimiting(@PathVariable String id) {
        return R.ok(sysAnnexService.get(id));
    }

    @Operation(summary = "获取文件URL集合")
    @PostMapping("annex-list")
    @HasAuthority("oauth")
    public R<List<AnnexListResponse>> annexList(@RequestBody List<String> ids) {
        AssertUtils.notEmpty(ids, "主键不能为空");
        return R.ok(sysAnnexService.get(ids));
    }

    @DeleteMapping("delete")
    @Operation(summary = "删除附件")
    @HasAuthority("oauth")
    public R<Object> deleteByIds(@RequestBody List<String> ids) {
        AssertUtils.notEmpty(ids, "主键不能为空");
        sysAnnexService.deleteByIds(ids);
        return R.ok();
    }
}

