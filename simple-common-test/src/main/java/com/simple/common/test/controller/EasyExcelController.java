package com.simple.common.test.controller;

import cn.hutool.core.date.DateTime;
import com.simple.common.annex.common.dto.UploadResponse;
import com.simple.common.annex.common.enums.ShareType;
import com.simple.common.annex.common.service.AnnexService;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.FileUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.excel.common.handler.DefaultEasyExcelReadHandler;
import com.simple.common.excel.common.service.EasyExcelReadService;
import com.simple.common.excel.common.service.EasyExcelWriteService;
import com.simple.common.test.common.entity.excel.EasyExcelDemo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@RequestMapping("easy-excel")
@Tag(name = "easy-excel")
@RestController
public class EasyExcelController {

    @Autowired
    private EasyExcelWriteService easyExcelWriteService;

    @Autowired
    private EasyExcelReadService easyExcelReadService;

    @Autowired
    private AnnexService annexService;

    @Operation(summary = "导出")
    @GetMapping("export")
    public void export() {

        List<EasyExcelDemo> list = new ArrayList<>();

        for (int i = 0; i < 4000; i++) {
            EasyExcelDemo sysAreaEntity = new EasyExcelDemo();
            sysAreaEntity.setName("name" + i);
            sysAreaEntity.setParentCode("code" + i);
            sysAreaEntity.setCode("AreaCode" + i);
            sysAreaEntity.setCreateTime(DateTime.now());
            list.add(sysAreaEntity);
        }
        easyExcelWriteService.writeResponse(EasyExcelDemo.class, list, "测试导出");

    }

    @Operation(summary = "导入")
    @PostMapping("import")
    public R<Object> importExcel(MultipartFile file) {
        easyExcelReadService.read(file, 2, EasyExcelDemo.class, new DefaultEasyExcelReadHandler<>(2000) {
            @Override
            protected void saveData(List<EasyExcelDemo> cachedDataList) {
                cachedDataList.forEach(easyExcelDemo -> {
                    if (log.isDebugEnabled()) {
                        log.debug("读取到数据：[{}]", JsonUtils.toJsonStr(easyExcelDemo));
                    }
                });
            }
        });
        return R.ok();
    }

    @Operation(summary = "导出并上传")
    @GetMapping("exportUrl")
    public R<UploadResponse> exportUrl() {

        List<EasyExcelDemo> list = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            EasyExcelDemo sysAreaEntity = new EasyExcelDemo();
            sysAreaEntity.setName("name" + i);
            sysAreaEntity.setParentCode("code" + i);
            sysAreaEntity.setCode("AreaCode" + i);
            sysAreaEntity.setCreateTime(DateTime.now());
            list.add(sysAreaEntity);
        }
        ByteArrayInputStream byteArrayInputStream1 = easyExcelWriteService.writeInputStream(EasyExcelDemo.class, list);
        UploadResponse simple = annexService.upload("区域列表.xlsx", "simple", ShareType.PUBLIC, byteArrayInputStream1);
        return R.ok(simple);
    }

    @Operation(summary = "导出到本地")
    @GetMapping("exportLocal")
    public R<Object> exportLocal() {

        List<EasyExcelDemo> list = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            EasyExcelDemo sysAreaEntity = new EasyExcelDemo();
            sysAreaEntity.setName("name" + i);
            sysAreaEntity.setParentCode("code" + i);
            sysAreaEntity.setCode("AreaCode" + i);
            sysAreaEntity.setCreateTime(DateTime.now());
            list.add(sysAreaEntity);
        }
        ByteArrayInputStream byteArrayInputStream1 = easyExcelWriteService.writeInputStream(EasyExcelDemo.class, list);
        FileUtils.write(byteArrayInputStream1, "D:/123.xlsx");
        return R.ok();
    }
}
