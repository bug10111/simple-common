package com.simple.common.logs.server.view;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.logs.server.common.dto.FindAllSysOperationLogsRequest;
import com.simple.common.logs.server.common.dto.FindSysOperationLogsRequest;
import com.simple.common.logs.server.common.entity.SysOperationLogs;
import com.simple.common.logs.server.common.view.SysOperationLogsView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 操作日志(sys_operation_logs)MyBatis-Plus视图实现
 *
 * @author qty
 */
@Repository
public class MPSysOperationLogsView implements SysOperationLogsView {

    @Autowired
    private SysOperationLogsRepository repository;

    @Override
    public IPage<SysOperationLogs> findAll(FindAllSysOperationLogsRequest findAllRequest) {
        LambdaQueryWrapper<SysOperationLogs> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(ObjUtil.isNotEmpty(findAllRequest.getTitle()), SysOperationLogs::getOperName, findAllRequest.getTitle())
                    .likeRight(ObjUtil.isNotEmpty(findAllRequest.getMethod()), SysOperationLogs::getMethod, findAllRequest.getMethod())
                    .likeRight(ObjUtil.isNotEmpty(findAllRequest.getOperUrl()), SysOperationLogs::getOperUrl, findAllRequest.getOperUrl())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getOperIp()), SysOperationLogs::getOperIp, findAllRequest.getOperIp())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getUserId()), SysOperationLogs::getUserId, findAllRequest.getUserId())
                    .likeRight(ObjUtil.isNotEmpty(findAllRequest.getNickname()), SysOperationLogs::getUserName, findAllRequest.getNickname())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getStatus()), SysOperationLogs::getStatus, findAllRequest.getStatus())
                    .orderByDesc(SysOperationLogs::getCreateTime);
        return repository.selectPage(findAllRequest.getPage(SysOperationLogs.class), queryWrapper);
    }

    @Override
    public SysOperationLogs findById(String id) {
        return repository.selectById(id);
    }

    @Override
    public SysOperationLogs findOne(FindSysOperationLogsRequest findRequest, FindSysOperationLogsRequest neRequest) {
        LambdaQueryWrapper<SysOperationLogs> queryWrapper = getWrapper(findRequest, neRequest);
        List<SysOperationLogs> list = repository.selectList(queryWrapper);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public List<SysOperationLogs> findList(FindSysOperationLogsRequest findRequest, FindSysOperationLogsRequest neRequest) {
        LambdaQueryWrapper<SysOperationLogs> queryWrapper = getWrapper(findRequest, neRequest);
        return repository.selectList(queryWrapper);
    }

    @Override
    public void save(SysOperationLogs logs) {
        repository.insert(logs);
    }

    @Override
    public void saves(List<SysOperationLogs> logsList) {
        repository.insert(logsList);
    }

    @Override
    public void updateById(SysOperationLogs logs) {
        repository.updateById(logs);
    }

    @Override
    public void update(SysOperationLogs logs, FindSysOperationLogsRequest findRequest, FindSysOperationLogsRequest neRequest) {
        LambdaQueryWrapper<SysOperationLogs> queryWrapper = getWrapper(findRequest, neRequest);
        repository.update(logs, queryWrapper);
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

    private LambdaQueryWrapper<SysOperationLogs> getWrapper(FindSysOperationLogsRequest findRequest, FindSysOperationLogsRequest neRequest) {
        LambdaQueryWrapper<SysOperationLogs> queryWrapper = new LambdaQueryWrapper<>();
        if (findRequest != null) {
            queryWrapper.eq(ObjUtil.isNotEmpty(findRequest.getId()), SysOperationLogs::getId, findRequest.getId())
                        .eq(ObjUtil.isNotEmpty(findRequest.getTitle()), SysOperationLogs::getOperName, findRequest.getTitle())
                        .eq(ObjUtil.isNotEmpty(findRequest.getMethod()), SysOperationLogs::getMethod, findRequest.getMethod())
                        .eq(ObjUtil.isNotEmpty(findRequest.getOperUrl()), SysOperationLogs::getOperUrl, findRequest.getOperUrl())
                        .eq(ObjUtil.isNotEmpty(findRequest.getOperIp()), SysOperationLogs::getOperIp, findRequest.getOperIp())
                        .eq(ObjUtil.isNotEmpty(findRequest.getUserId()), SysOperationLogs::getUserId, findRequest.getUserId());
        }
        if (neRequest != null) {
            queryWrapper.ne(ObjUtil.isNotEmpty(neRequest.getId()), SysOperationLogs::getId, neRequest.getId())
                        .ne(ObjUtil.isNotEmpty(neRequest.getTitle()), SysOperationLogs::getOperName, neRequest.getTitle())
                        .ne(ObjUtil.isNotEmpty(neRequest.getMethod()), SysOperationLogs::getMethod, neRequest.getMethod())
                        .ne(ObjUtil.isNotEmpty(neRequest.getOperUrl()), SysOperationLogs::getOperUrl, neRequest.getOperUrl())
                        .ne(ObjUtil.isNotEmpty(neRequest.getOperIp()), SysOperationLogs::getOperIp, neRequest.getOperIp())
                        .ne(ObjUtil.isNotEmpty(neRequest.getUserId()), SysOperationLogs::getUserId, neRequest.getUserId());
        }
        return queryWrapper;
    }
}