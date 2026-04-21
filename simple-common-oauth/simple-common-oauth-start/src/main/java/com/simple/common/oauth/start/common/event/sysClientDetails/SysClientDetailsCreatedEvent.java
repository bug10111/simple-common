package com.simple.common.oauth.start.common.event.sysClientDetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.eventbus.common.annotation.Event;
import com.simple.common.eventbus.common.constants.EventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 客户端信息(sys_client_details)创建事件
 *
 * @author qty
 */
@Data
@Event(targets = EventConstant.TARGET_ALL_X)
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class SysClientDetailsCreatedEvent {

    @Schema(description = "客户端（如：xiaoyue_client）")
    private String clientId;

    @Schema(description = "服务")
    private String server;

    @Schema(description = "32位密钥字符串")
    private String hsKey;

    @Schema(description = "RSA公钥")
    private String rsaPublic;

    @Schema(description = "RSA私钥")
    private String rsaPrivate;
}

