package com.simple.common.test.controller;

import com.simple.common.core.utils.QRCodeUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@RequestMapping("qr")
@Tag(name = "二维码异步批量生成")
@RestController
public class QRCodeAndThreadController {

    @Operation(summary = "批量生成")
    @PostMapping("create-list")
    public void createList(Integer size) {
        QRCodeUtils.createZip(null, size);
    }

    @Operation(summary = "单个生成")
    @PostMapping("create")
    public void create(String text) {
        QRCodeUtils.create(text,null,text);
    }

}
