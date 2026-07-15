package com.simple.common.test.controller;

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
 * WebSocket测试控制器。
 * <p>
 * 演示 {@link WebSocketListening} 注解的两种用法：
 * 直接返回值（框架自动发送）和使用 {@link WebSocketRequest#reply(String)} 主动推送进度。
 * </p>
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

    /**
     * 简单模式：直接返回值，框架自动将返回值发送给当前客户端。
     * cliKey 默认为 "default"，作为该 type 下所有消息的兜底处理器。
     */
    @WebSocketListening(type = type)
    public String handler(WebSocketRequest<String> request) {
        log.info("接收到信息：[type={}, cliKey={}, data={}]", request.getType(), request.getCliKey(), request.getData());
        return "msg:ok";
    }

    /**
     * 进度推送模式：通过 request.reply() 在处理过程中向当前客户端推送中间消息。
     */
    @WebSocketListening(type = "chat")
    public void chatHandler(WebSocketRequest<String> request) {
        log.info("chat 收到消息 [cliKey={}]: {}", request.getCliKey(), request.getData());
        // 推送处理进度（点对点，只发给当前客户端）
        request.reply("{\"status\":\"processing\",\"progress\":30}");
        // 模拟耗时处理
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // 推送最终结果
        request.reply("{\"status\":\"done\",\"result\":\"处理完成: " + request.getData() + "\"}");
    }

}