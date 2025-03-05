package com.simple.common.logs.server.common.dto;

import java.util.Date;

import com.simple.common.mp.page.PageBase;
import io.swagger.v3.oas.annotations.media.Schema;
import com.simple.common.mp.common.enums.DeleteState;
import com.simple.common.mp.common.enums.Status;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(title = "操作日志(sys_operation_logs)列表请求参数")
public class FindAllSysOperationLogsRequest extends PageBase {

    @Schema(description = "方法名称")
    private String title;

    @Schema(description = "请求方式")
    private String method;

    @Schema(description = "请求URL")
    private String operUrl;

    @Schema(description = "主机地址")
    private String operIp;

    @Schema(description = "操作人员id")
    private String userId;

    @Schema(description = "用户名")
    private String nickname;

    @Schema(description = "操作状态")
    private Integer status;
}

