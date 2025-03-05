package com.simple.common.logs.server.common.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.mp.common.enums.DeleteState;
import com.simple.common.mp.common.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

import lombok.experimental.Accessors;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
@Schema(title = "操作日志(sys_operation_logs)明细响应")
public class SysOperationLogsInfoResponse {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "方法名称")
    private String title;

    @Schema(description = "请求方式")
    private String method;

    @Schema(description = "请求URL")
    private String operUrl;

    @Schema(description = "主机地址")
    private String operIp;

    @Schema(description = "操作地点")
    private String operLocation;

    @Schema(description = "操作人员id")
    private Integer userId;

    @Schema(description = "请求参数")
    private String operParam;

    @Schema(description = "操作状态")
    private Status status;

    @Schema(description = "错误消息")
    private String errorMsg;

    @Schema(description = "接口耗时")
    private Integer requestTime;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "修改时间")
    private Date updateTime;
}

