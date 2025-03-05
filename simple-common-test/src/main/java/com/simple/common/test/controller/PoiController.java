package com.simple.common.test.controller;

import cn.hutool.core.date.DateTime;
import com.simple.common.annex.common.dto.UploadResponse;
import com.simple.common.annex.common.enums.ShareType;
import com.simple.common.annex.common.service.AnnexService;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.excel.common.function.PoiExportFunction;
import com.simple.common.excel.common.handler.DefaultPoiReadHandler;
import com.simple.common.excel.common.service.PoiReadService;
import com.simple.common.excel.common.service.PoiWriteService;
import com.simple.common.test.common.entity.excel.SysAreaEntity;
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
 * @author 兄台丶请冷静
 */
@Slf4j
@RequestMapping("poi")
@Tag(name = "poi")
@RestController
public class PoiController {

    @Autowired
    private PoiWriteService poiWriteService;

    @Autowired
    private PoiReadService poiReadService;

    @Autowired
    private AnnexService annexService;

    @Operation(summary = "导出")
    @GetMapping("export")
    public void export() {

        List<SysAreaEntity> list = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            SysAreaEntity sysAreaEntity = new SysAreaEntity();
            sysAreaEntity.setAreaName("name" + i);
            sysAreaEntity.setParentCode("code" + i);
            sysAreaEntity.setAreaCode("AreaCode" + i);
            sysAreaEntity.setAreaName("name" + i);
            list.add(sysAreaEntity);
        }

        //初始化表头和列宽度
        String[] head = { "名称", "父编码", "编码", "创建时间" };
        Integer[] wi = { 30 * 100 };
        PoiExportFunction<SysAreaEntity> function = (row, entity) -> {
            row.createCell(0).setCellValue(entity.getAreaName());
            row.createCell(1).setCellValue(entity.getParentCode());
            row.createCell(2).setCellValue(entity.getAreaCode());
            row.createCell(3).setCellValue(DateTime.now().toString());
        };
        poiWriteService.exportResponse(function, list, head, wi, 1000000, "区域列表");
    }

    @Operation(summary = "导入")
    @PostMapping("import")
    public R<Object> importExcel(MultipartFile file) {
        DefaultPoiReadHandler<SysAreaEntity> defaultPoiReadHandler = new DefaultPoiReadHandler<>(2, 1, 4) {
            @Override
            public SysAreaEntity handler(String[] row) {
                SysAreaEntity sysAreaEntity = new SysAreaEntity();
                sysAreaEntity.setAreaName(row[0]);
                sysAreaEntity.setParentCode(row[1]);
                sysAreaEntity.setAreaCode(row[2]);
                sysAreaEntity.setAreaName(row[3]);
                AssertUtils.error("123");
                return sysAreaEntity;
            }
        };
        poiReadService.read(file, defaultPoiReadHandler);
        if (defaultPoiReadHandler.getResults()) {
            log.info("读取excel成功");
            for (SysAreaEntity entity : defaultPoiReadHandler.getList()) {
                log.info(JsonUtils.toJsonStr(entity));
            }
        } else {
            return R.ok(defaultPoiReadHandler.getError());
        }
        return R.ok();
    }

    @Operation(summary = "导出并上传")
    @GetMapping("exportUrl")
    public R<UploadResponse> exportUrl() {

        List<SysAreaEntity> list = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            SysAreaEntity sysAreaEntity = new SysAreaEntity();
            sysAreaEntity.setAreaName("name" + i);
            sysAreaEntity.setParentCode("code" + i);
            sysAreaEntity.setAreaCode("AreaCode" + i);
            sysAreaEntity.setAreaName("name" + i);
            list.add(sysAreaEntity);
        }

        //初始化表头和列宽度
        String[] head = { "名称", "父编码", "编码", "创建时间" };
        Integer[] wi = { 30 * 100 };
        PoiExportFunction<SysAreaEntity> function = (row, entity) -> {
            row.createCell(0).setCellValue(entity.getAreaName());
            row.createCell(1).setCellValue(entity.getParentCode());
            row.createCell(2).setCellValue(entity.getAreaCode());
            row.createCell(3).setCellValue(DateTime.now().toString());
        };
        ByteArrayInputStream byteArrayInputStream = poiWriteService.writeInputStream(function, list, head, wi, 1000000);
        UploadResponse simple = annexService.upload("区域列表.xlsx", "simple", null, ShareType.PUBLIC, byteArrayInputStream);
        return R.ok(simple);
    }
}
