package com.simple.common.logs.server.common.dto;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(title = "操作日志(sys_operation_logs)修改请求参数")
public class UpdateSysOperationLogsRequest extends CreateSysOperationLogsRequest {

    @Schema(description = "主键")
    @NotEmpty(message = "主键不能为空")
    private String id;

}

