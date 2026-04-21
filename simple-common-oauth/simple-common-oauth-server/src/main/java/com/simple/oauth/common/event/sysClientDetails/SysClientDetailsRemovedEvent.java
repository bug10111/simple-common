package com.simple.oauth.common.event.sysClientDetails;

import java.util.Date;

import com.simple.common.eventbus.common.constants.EventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.eventbus.common.annotation.Event;
import lombok.experimental.Accessors;

/**
 * 客户端信息(sys_client_details)删除事件
 *
 * @author qty
 */
@Data
@Event(targets = EventConstant.TARGET_ALL_X)
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class SysClientDetailsRemovedEvent {

    @Schema(description = "客户端（如：xiaoyue_client）")
    private String clientId;

    @Schema(description = "服务")
    private String server;

}

