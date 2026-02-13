package com.simple.common.test.controller;

import cn.hutool.core.util.ObjUtil;
import com.simple.common.core.common.service.thread.ThreadService;
import com.simple.common.core.utils.QRCodeUtils;
import com.simple.common.core.utils.ThreadUtils;
import com.simple.common.core.utils.ZipUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
