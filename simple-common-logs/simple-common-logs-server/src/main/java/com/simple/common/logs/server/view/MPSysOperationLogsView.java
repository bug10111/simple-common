package com.simple.common.logs.server.view;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.logs.server.common.dto.FindAllSysOperationLogsRequest;
import com.simple.common.logs.server.common.dto.FindSysOperationLogsRequest;
import com.simple.common.logs.server.common.entity.SysOperationLogs;
import com.simple.common.logs.server.common.view.SysOperationLogsView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 操作日志(sys_operation_logs)数据库视图实现
 *
 * @author 兄台丶请冷静
 */
@Component
class MPSysOperationLogsView implements SysOperationLogsView {

    @Autowired
    private SysOperationLogsRepository repository;

    @Override
    public IPage<SysOperationLogs> findAll(FindAllSysOperationLogsRequest findAllRequest) {
        LambdaQueryWrapper<SysOperationLogs> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(ObjUtil.isNotEmpty(findAllRequest.getTitle()), SysOperationLogs::getTitle, findAllRequest.getTitle())
                    .likeRight(ObjUtil.isNotEmpty(findAllRequest.getMethod()), SysOperationLogs::getMethod, findAllRequest.getMethod())
                    .likeRight(ObjUtil.isNotEmpty(findAllRequest.getOperUrl()), SysOperationLogs::getOperUrl, findAllRequest.getOperUrl())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getOperIp()), SysOperationLogs::getOperIp, findAllRequest.getOperIp())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getUserId()), SysOperationLogs::getUserId, findAllRequest.getUserId())
                    .likeRight(ObjUtil.isNotEmpty(findAllRequest.getNickname()), SysOperationLogs::getNickname, findAllRequest.getNickname())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getStatus()), SysOperationLogs::getStatus, findAllRequest.getStatus())
    ;
        return repository.selectPage(findAllRequest.getPage(SysOperationLogs.class), queryWrapper);
    }

    @Override
    public SysOperationLogs findOne(FindSysOperationLogsRequest findRequest, FindSysOperationLogsRequest neRequest) {
        LambdaQueryWrapper<SysOperationLogs> queryWrapper = getWrapper(findRequest, neRequest);
        List<SysOperationLogs> list = repository.selectList(queryWrapper);
        if (list.isEmpty()) {
            return null;
        } else if (list.size() > 1) {
            AssertUtils.errorParams("数据异常", "参数为[{}]查询只需要一条数据，但是查询出来多条", JsonUtils.toJsonStr(findRequest));
        }
        return list.get(0);
    }

    @Override
    public List<SysOperationLogs> findList(FindSysOperationLogsRequest findRequest, FindSysOperationLogsRequest neRequest) {
        LambdaQueryWrapper<SysOperationLogs> queryWrapper = getWrapper(findRequest, neRequest);
        return repository.selectList(queryWrapper);
    }

    @Override
    public SysOperationLogs findById(String id) {
        return repository.selectById(id);
    }

    @Override
    public void updateById(SysOperationLogs sysOperationLogs) {
        repository.updateById(sysOperationLogs);
    }

    @Override
    public void update(SysOperationLogs sysOperationLogs, FindSysOperationLogsRequest findRequest, FindSysOperationLogsRequest neRequest) {
        LambdaQueryWrapper<SysOperationLogs> queryWrapper = getWrapper(findRequest, neRequest);
        repository.update(sysOperationLogs, queryWrapper);
    }

    @Override
    public void save(SysOperationLogs sysOperationLogs) {
        repository.insert(sysOperationLogs);
    }

    @Override
    public void saves(List<SysOperationLogs> list) {
        repository.insertBatch(list);
    }

    @Override
    public void delete(FindSysOperationLogsRequest findRequest, FindSysOperationLogsRequest neRequest) {
        LambdaQueryWrapper<SysOperationLogs> queryWrapper = getWrapper(findRequest, neRequest);
        repository.delete(queryWrapper);
    }

    @Override
    public void deleteByIds(List<String> ids) {
        repository.deleteByIds(ids);
    }

    protected LambdaQueryWrapper<SysOperationLogs> getWrapper(FindSysOperationLogsRequest findRequest, FindSysOperationLogsRequest neRequest) {
        LambdaQueryWrapper<SysOperationLogs> queryWrapper = new LambdaQueryWrapper<>();

        if (findRequest != null) {
            queryWrapper.eq(ObjUtil.isNotEmpty(findRequest.getTitle()), SysOperationLogs::getTitle, findRequest.getTitle())
                        .eq(ObjUtil.isNotEmpty(findRequest.getMethod()), SysOperationLogs::getMethod, findRequest.getMethod())
                        .eq(ObjUtil.isNotEmpty(findRequest.getOperUrl()), SysOperationLogs::getOperUrl, findRequest.getOperUrl())
                        .eq(ObjUtil.isNotEmpty(findRequest.getOperIp()), SysOperationLogs::getOperIp, findRequest.getOperIp())
                        .eq(ObjUtil.isNotEmpty(findRequest.getOperLocation()), SysOperationLogs::getOperLocation, findRequest.getOperLocation())
                        .eq(ObjUtil.isNotEmpty(findRequest.getUserId()), SysOperationLogs::getUserId, findRequest.getUserId())
                        .eq(ObjUtil.isNotEmpty(findRequest.getOperParam()), SysOperationLogs::getOperParam, findRequest.getOperParam())
                        .eq(ObjUtil.isNotEmpty(findRequest.getStatus()), SysOperationLogs::getStatus, findRequest.getStatus())
                        .eq(ObjUtil.isNotEmpty(findRequest.getErrorMsg()), SysOperationLogs::getErrorMsg, findRequest.getErrorMsg())
                        .eq(ObjUtil.isNotEmpty(findRequest.getRequestTime()), SysOperationLogs::getRequestTime, findRequest.getRequestTime());
        }

        if (neRequest != null) {
            queryWrapper.ne(ObjUtil.isNotEmpty(neRequest.getId()), SysOperationLogs::getId, neRequest.getId())
                        .ne(ObjUtil.isNotEmpty(neRequest.getTitle()), SysOperationLogs::getTitle, neRequest.getTitle())
                        .ne(ObjUtil.isNotEmpty(neRequest.getMethod()), SysOperationLogs::getMethod, neRequest.getMethod())
                        .ne(ObjUtil.isNotEmpty(neRequest.getOperUrl()), SysOperationLogs::getOperUrl, neRequest.getOperUrl())
                        .ne(ObjUtil.isNotEmpty(neRequest.getOperIp()), SysOperationLogs::getOperIp, neRequest.getOperIp())
                        .ne(ObjUtil.isNotEmpty(neRequest.getOperLocation()), SysOperationLogs::getOperLocation, neRequest.getOperLocation())
                        .ne(ObjUtil.isNotEmpty(neRequest.getUserId()), SysOperationLogs::getUserId, neRequest.getUserId())
                        .ne(ObjUtil.isNotEmpty(neRequest.getOperParam()), SysOperationLogs::getOperParam, neRequest.getOperParam())
                        .ne(ObjUtil.isNotEmpty(neRequest.getStatus()), SysOperationLogs::getStatus, neRequest.getStatus())
                        .ne(ObjUtil.isNotEmpty(neRequest.getErrorMsg()), SysOperationLogs::getErrorMsg, neRequest.getErrorMsg())
                        .ne(ObjUtil.isNotEmpty(neRequest.getRequestTime()), SysOperationLogs::getRequestTime, neRequest.getRequestTime());
        }
        return queryWrapper;
    }
}

