package com.simple.common.logs.server.service;

import com.simple.common.core.utils.BeanUtils;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.eventbus.common.service.EventBusService;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.simple.common.logs.server.common.service.SysOperationLogsService;
import com.simple.common.logs.server.common.entity.SysOperationLogs;
import com.simple.common.logs.server.common.view.SysOperationLogsView;
import com.simple.common.logs.server.common.dto.SysOperationLogsPageResponse;
import com.simple.common.logs.server.common.dto.SysOperationLogsInfoResponse;
import com.simple.common.logs.server.common.dto.CreateSysOperationLogsRequest;
import com.simple.common.logs.server.common.dto.UpdateSysOperationLogsRequest;
import com.simple.common.logs.server.common.dto.FindAllSysOperationLogsRequest;

import java.util.List;

/**
 * 操作日志(sys_operation_logs)默认接口实现
 *
 * @author 兄台丶请冷静
 */
@Service
@Transactional
class DefaultSysOperationLogsService implements SysOperationLogsService {

    @Autowired
    private SysOperationLogsView sysOperationLogsView;

    @Autowired
    private EventBusService eventBusService;

    @Override
    public IPage<SysOperationLogsPageResponse> findAll(FindAllSysOperationLogsRequest findAllRequest) {
        var pageInfo = sysOperationLogsView.findAll(findAllRequest);
        return pageInfo.convert(entity -> BeanUtils.copyProperties(entity, SysOperationLogsPageResponse.class));
    }

    @Override
    public SysOperationLogsInfoResponse findById(String id) {
        var sysOperationLogs = sysOperationLogsView.findById(id);
        AssertUtils.notEmptyParams(sysOperationLogs, "主键为[{}]的数据为空", id);
        return BeanUtils.copyProperties(sysOperationLogs, SysOperationLogsInfoResponse.class);
    }

    @Override
    public String save(CreateSysOperationLogsRequest createRequest) {
        var entity = BeanUtils.copyProperties(createRequest, SysOperationLogs.class);
        sysOperationLogsView.save(entity);
        return entity.getId();
    }

    @Override
    public String updateById(UpdateSysOperationLogsRequest updateRequest) {
        var entity = BeanUtils.copyProperties(updateRequest, SysOperationLogs.class);
        sysOperationLogsView.updateById(entity);
        return entity.getId();
    }

    @Override
    public void deleteByIds(List<String> ids) {
        sysOperationLogsView.deleteByIds(ids);
    }
}

