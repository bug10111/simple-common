package com.simple.common.test.common.entity.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
@Schema(description = "")
public class AiRequest {
    private String sessionId;

    private String userMessage;
}
