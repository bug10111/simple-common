package com.simple.common.logs.server.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
@Schema(title = "操作日志(sys_operation_logs)明细响应")
public class SysOperationLogsInfoResponse {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "模块名称")
    private String module;

    @Schema(description = "操作类型")
    private String operType;

    @Schema(description = "请求方式")
    private String method;

    @Schema(description = "请求URL")
    private String operUrl;

    @Schema(description = "主机地址")
    private String operIp;

    @Schema(description = "操作地点")
    private String operLocation;

    @Schema(description = "操作名称")
    private String operName;

    @Schema(description = "请求参数")
    private String operParam;

    @Schema(description = "返回结果")
    private String operResult;

    @Schema(description = "错误消息")
    private String errorMessage;

    @Schema(description = "操作时间")
    private LocalDateTime operTime;

    @Schema(description = "操作用户ID")
    private Long userId;

    @Schema(description = "操作用户名")
    private String userName;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "请求方法")
    private String requestMethod;

    @Schema(description = "请求耗时（毫秒）")
    private Long costTime;

    @Schema(description = "客户端标识")
    private String clientId;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}