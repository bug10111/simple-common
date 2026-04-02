package com.simple.oauth.common.dto.sysClientDetails;

import com.simple.common.mp.common.enums.Status;
import com.simple.common.mp.page.PageBase;
import com.simple.oauth.common.enums.ServerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
@Schema(title = "客户端信息(sys_client_details)列表请求参数")
public class FindAllSysClientDetailsRequest extends PageBase {

    @Schema(description = "客户端名称")
    private String clientName;

    @Schema(description = "服务")
    private String server;

    @Schema(description = "服务类型")
    private ServerType serverType;

    @Schema(description = "是否有微信小程序")
    private Boolean hasWx;

    @Schema(description = "状态")
    private Status status;

}

