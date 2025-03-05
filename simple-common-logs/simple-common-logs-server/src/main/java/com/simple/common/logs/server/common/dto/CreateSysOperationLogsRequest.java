package com.simple.common.logs.server.common.dto;

import java.util.Date;

import com.simple.common.mp.common.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
@Schema(title = "操作日志(sys_operation_logs)创建请求参数")
public class CreateSysOperationLogsRequest {

    @Schema(description = "方法名称")
    @NotEmpty(message = "方法名称不能为空")
    private String title;

    @Schema(description = "请求方式")
    @NotEmpty(message = "请求方式不能为空")
    private String method;

    @Schema(description = "请求URL")
    @NotEmpty(message = "请求URL不能为空")
    private String operUrl;

    @Schema(description = "主机地址")
    @NotEmpty(message = "主机地址不能为空")
    private String operIp;

    @Schema(description = "操作地点")
    @NotEmpty(message = "操作地点不能为空")
    private String operLocation;

    @Schema(description = "操作人员id")
    @NotNull(message = "操作人员id不能为空")
    private Integer userId;

    @Schema(description = "请求参数")
    @NotEmpty(message = "请求参数不能为空")
    private String operParam;

    @Schema(description = "错误消息")
    @NotEmpty(message = "错误消息不能为空")
    private String errorMsg;

    @Schema(description = "接口耗时")
    @NotNull(message = "接口耗时不能为空")
    private Integer requestTime;
}

