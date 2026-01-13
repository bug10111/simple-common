package com.simple.common.logs.server.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.core.response.R;
import com.simple.common.logs.server.common.dto.FindAllSysOperationLogsRequest;
import com.simple.common.logs.server.common.dto.SysOperationLogsPageResponse;
import com.simple.common.logs.server.common.service.SysOperationLogsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志(sys_operation_logs)控制层
 *
 * @author qty
 */
@Slf4j
@Tag(name = "操作日志")
@RequestMapping("auth/sys-operation-logs")
@RestController
public class SysOperationLogsController {

    @Autowired
    private SysOperationLogsService sysOperationLogsService;

    @GetMapping("list")
    @Operation(summary = "分页查询操作日志")
    public R<IPage<SysOperationLogsPageResponse>> list(@ParameterObject FindAllSysOperationLogsRequest findAllRequest) {
        return R.ok(sysOperationLogsService.findAll(findAllRequest));
    }

}

