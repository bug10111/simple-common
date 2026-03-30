package com.simple.common.ai.common.builder;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simple.common.ai.common.properties.AiAliProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Accessors(chain = true)
@Schema(description = "阿里Ai对话构造器")
public class AiAliBuilder<T> {

    private volatile AiAliProperties aiAliProperties = null;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> AiAliBuilder<T> builder() {
        return new AiAliBuilder<>();
    }
    /**
     * Ai对话
     */
    @SneakyThrows
    public AiAliBuilder<T>.AiResult sendMsg(String sessionId, String msg) {
        ApplicationParam param = ApplicationParam.builder().apiKey(get().getApiKey()).appId(get().getAppId()).prompt(msg).build();

        param.setSessionId(sessionId);
        param.setEnableThinking(get().isEnableThinking());

        Application application = new Application();
        ApplicationResult result = application.call(param);

        String aiReply = result.getOutput().getText();

        AiAliBuilder<T>.AiResult aiResult = this.new AiResult();
        aiResult.setMsg(aiReply);
        aiResult.setStructured(isJsonString(aiReply));
        return aiResult;
    }

    private boolean isJsonString(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        try {
            objectMapper.readTree(text.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private AiAliProperties get() {
        if (aiAliProperties == null) {
            synchronized (this) {
                if (aiAliProperties == null) {
                    aiAliProperties = SpringUtil.getBean(AiAliProperties.class);
                }
            }
        }
        return aiAliProperties;
    }

    @Data
    public class AiResult {

        @Schema(description = "是否是结构化数据")
        private boolean structured;

        @Schema(description = "内容")
        private String msg;

        @SneakyThrows
        public T toObj(Class<T> tClass) {
            return objectMapper.readValue(this.msg, tClass);
        }
    }
}