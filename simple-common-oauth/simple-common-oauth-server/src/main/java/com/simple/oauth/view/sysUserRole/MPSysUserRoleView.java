package com.simple.oauth.view.sysUserRole;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.simple.common.core.utils.AssertUtils;
import com.simple.oauth.common.entity.sysUserRole.SysUserRole;
import com.simple.oauth.common.view.sysUserRole.SysUserRoleView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户和角色关联(sys_user_role)数据库视图实现
 *
 * @author qty
 */
@Component
class MPSysUserRoleView implements SysUserRoleView {

    @Autowired
    private SysUserRoleRepository repository;

    @Override
    public SysUserRole findById(String id, Boolean allowEmpty) {
        SysUserRole sysUserRole = repository.selectById(id);
        if (!allowEmpty && sysUserRole == null) {
            AssertUtils.errorParams("主键为[{}]的数据为空", id);
        }
        return sysUserRole;
    }

    @Override
    public List<SysUserRole> findByUserId(String userId) {
        return repository.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
    }

    @Override
    public List<SysUserRole> findByRoleId(String roleId) {
        return repository.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
    }

    @Override
    public void saveOrUpdate(SysUserRole sysUserRole) {
        if (sysUserRole.getId() == null) {
            repository.insert(sysUserRole);
        } else {
            repository.updateById(sysUserRole);
        }
    }

    @Override
    public void saves(List<SysUserRole> list) {
        repository.insertBatch(list);
    }

    @Override
    public void deleteByIds(List<String> ids) {
        repository.deleteByIds(ids);
    }

    @Override
    public void deleteByUserId(String userId) {
        repository.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
    }

    @Override
    public void deleteByUserIdAndRoleId(String userId, String roleId) {
        repository.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId).eq(SysUserRole::getRoleId, roleId));
    }
}

