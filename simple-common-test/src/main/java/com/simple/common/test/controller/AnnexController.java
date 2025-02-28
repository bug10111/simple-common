package com.simple.common.test.controller;

import com.simple.common.annex.common.dto.UploadResponse;
import com.simple.common.annex.common.enums.ShareType;
import com.simple.common.annex.common.service.AnnexService;
import com.simple.common.core.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Created by IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@RequestMapping("annex")
@Tag(name = "附件上传")
@RestController
public class AnnexController {

    @Autowired
    private AnnexService annexService;

    @Operation(summary = "文件上传")
    @PostMapping("upload")
    public R<UploadResponse> upload(MultipartFile filter, ShareType shareType) {
        UploadResponse simple = annexService.upload(filter, "simple", shareType);
        // TODO: 2023/11/23 这里可以保存对象到数据库，md5值不在封装计算
        return R.ok(simple);
    }

    @Operation(summary = "获取文件")
    @GetMapping
    public R<Object> currentLimiting(String objectUrl) {
        // TODO: 2023/11/23 这里可以byId查询数据库
        return R.ok(annexService.generateUrl(objectUrl));
    }

    @Operation(summary = "下载文件")
    @GetMapping("writeGetObjectResponse")
    public R<Object> writeGetObjectResponse(String objectUrl) {
        annexService.writeGetObjectResponse(objectUrl);
        return R.ok();
    }

    @Operation(summary = "删除文件")
    @DeleteMapping
    public R<Object> delete(String objectUrl) {
        annexService.delete(objectUrl);
        return R.ok();
    }
}
