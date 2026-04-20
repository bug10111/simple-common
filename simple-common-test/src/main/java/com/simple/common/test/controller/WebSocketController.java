package com.simple.common.test.controller;

import com.alibaba.dashscope.utils.JsonUtils;
import com.simple.common.core.response.R;
import com.simple.common.websocket.common.annotation.WebSocketListening;
import com.simple.common.websocket.common.entity.WebSocketRequest;
import com.simple.common.websocket.utils.WebSocketUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public String handler(WebSocketRequest request) {
        log.info("接收到信息：[{}]", JsonUtils.toJson(request));
        return "msg:ok";
    }

}
