package com.simple.oauth.view.sysRole;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.oauth.common.dto.sysRole.FindAllSysRoleRequest;
import com.simple.oauth.common.dto.sysRole.FindOneSysRoleRequest;
import com.simple.oauth.common.dto.sysRole.SysRoleInfoResponse;
import com.simple.oauth.common.entity.sysRole.SysRole;
import com.simple.oauth.common.view.sysRole.SysRoleView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 角色信息(sys_role)数据库视图实现
 *
 * @author 兄台丶请冷静
 */
@Component
class MPSysRoleView implements SysRoleView {

    @Autowired
    private SysRoleRepository repository;

    @Override
    public IPage<SysRole> findAll(FindAllSysRoleRequest findAllRequest) {
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(ObjUtil.isNotEmpty(findAllRequest.getRoleName()), SysRole::getRoleName, findAllRequest.getRoleName())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getRoleKey()), SysRole::getRoleKey, findAllRequest.getRoleKey())
                    .in(ObjUtil.isNotEmpty(findAllRequest.getServer()),SysRole::getServer,findAllRequest.getServer())
        ;
        return repository.selectPage(findAllRequest.getPage(SysRole.class), queryWrapper);
    }

    @Override
    public List<SysRole> findAll() {
        return repository.selectList(new LambdaQueryWrapper<SysRole>().select(SysRole::getRoleKey));
    }

    @Override
    public List<SysRoleInfoResponse> findByUserIdAndServer(String userId,String server) {
        return repository.getRole(userId, server);
    }

    @Override
    public SysRole findOne(FindOneSysRoleRequest findOneRequest, FindOneSysRoleRequest neRequest, Boolean allowEmpty) {
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(findOneRequest.getId()), SysRole::getId, findOneRequest.getId())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getRoleName()), SysRole::getRoleName, findOneRequest.getRoleName())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getRoleKey()), SysRole::getRoleKey, findOneRequest.getRoleKey())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getSerial()), SysRole::getSerial, findOneRequest.getSerial())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getRemark()), SysRole::getRemark, findOneRequest.getRemark())
                    .ne(ObjUtil.isNotEmpty(neRequest.getId()), SysRole::getId, neRequest.getId())
                    .ne(ObjUtil.isNotEmpty(neRequest.getRoleName()), SysRole::getRoleName, neRequest.getRoleName())
                    .ne(ObjUtil.isNotEmpty(neRequest.getRoleKey()), SysRole::getRoleKey, neRequest.getRoleKey())
                    .ne(ObjUtil.isNotEmpty(neRequest.getSerial()), SysRole::getSerial, neRequest.getSerial())
                    .ne(ObjUtil.isNotEmpty(neRequest.getRemark()), SysRole::getRemark, neRequest.getRemark());

        List<SysRole> list = repository.selectList(queryWrapper);
        if (list.isEmpty()) {
            if (allowEmpty) {
                return null;
            } else {
                AssertUtils.error("数据为空", "参数为[{}]的查询没有数据", JsonUtils.toJsonStr(findOneRequest));
            }
        } else if (list.size() > 1) {
            AssertUtils.error("数据异常", "参数为[{}]查询只需要一条数据，但是查询出来多条", JsonUtils.toJsonStr(findOneRequest));
        }
        return list.get(0);
    }

    @Override
    public SysRole findById(String id, Boolean allowEmpty) {
        SysRole findById = repository.selectById(id);
        if (!allowEmpty && findById == null) {
            AssertUtils.errorParams("主键为[{}]的数据为空", id);
        }
        return findById;
    }

    @Override
    public void saveOrUpdate(SysRole sysRole) {
        sysRole.setUpdateTime(DateTime.now());
        if (sysRole.getId() == null) {
            sysRole.setCreateTime(DateTime.now());
            repository.insert(sysRole);
        } else {
            repository.updateById(sysRole);
        }
    }

    @Override
    public void saves(List<SysRole> list) {
        repository.insertBatch(list);
    }

    @Override
    public void deleteByIds(List<String> ids) {
        repository.deleteByIds(ids);
    }
}

