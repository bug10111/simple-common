package com.simple.common.test.controller;

import com.simple.common.annex.common.dto.UploadResponse;
import com.simple.common.annex.common.enums.ShareType;
import com.simple.common.annex.common.service.AnnexService;
import com.simple.common.core.response.R;
import com.simple.common.websocket.common.annotation.WebSocketListening;
import com.simple.common.websocket.utils.WebSocketUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Created by IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@RequestMapping("websocket")
@Tag(name = "websocket")
@RestController
public class WebSocketController {

    private static final String type = "order";
    private static final String cli = "default";

    @Operation(summary = "主动发送信息")
    @PostMapping("send")
    public R<Object> send(String msg) {
        WebSocketUtils.sendMsg(type, cli, msg);
        return R.ok();
    }

//    @WebSocketListening(type = type)
//    public void handler(String msg) {
//        log.info("接收到信息：[{}]",msg);
//    }

    @WebSocketListening(type = type)
    public String handler(String msg) {
        log.info("接收到信息：[{}]",msg);
        return "msg:ok";
    }

}
