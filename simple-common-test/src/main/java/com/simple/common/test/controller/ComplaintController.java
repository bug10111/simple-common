package com.simple.common.test.controller;

import cn.hutool.core.util.ObjUtil;
import com.simple.common.ai.common.builder.AiAliBuilder;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.IdUtils;
import com.simple.common.test.common.entity.ai.AiRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/complaint")
public class ComplaintController {

    @PostMapping("/chat")
    public R<Object> chat(@RequestBody AiRequest request) {
        String sessionId = ObjUtil.isEmpty(request.getSessionId()) ? IdUtils.getFastSimpleUUID() : request.getSessionId();
        String message = request.getUserMessage();

        AiAliBuilder<Object>.AiResult aiResult = AiAliBuilder.builder().sendMsg(sessionId, message);
        if (aiResult.isStructured()) {
            log.info("接收到结构化数据[{}]", aiResult.getMsg());
            return R.ok("感谢你的反馈");
        }
        return R.ok(Map.of("sessionId", sessionId, "reply", aiResult.getMsg()));
    }

}