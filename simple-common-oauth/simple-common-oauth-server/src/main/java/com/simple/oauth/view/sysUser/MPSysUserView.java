package com.simple.oauth.view.sysUser;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.oauth.common.dto.sysUser.FindAllSysUserRequest;
import com.simple.oauth.common.dto.sysUser.FindOneSysUserRequest;
import com.simple.oauth.common.entity.sysUser.SysUser;
import com.simple.oauth.common.view.sysUser.SysUserView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户(sys_user)数据库视图实现
 *
 * @author 兄台丶请冷静
 */
@Component
class MPSysUserView implements SysUserView {

    @Autowired
    private SysUserRepository repository;

    @Override
    public IPage<SysUser> findAll(FindAllSysUserRequest findAllRequest) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(ObjUtil.isNotEmpty(findAllRequest.getUsername()), SysUser::getUsername, findAllRequest.getUsername())
                    .likeRight(ObjUtil.isNotEmpty(findAllRequest.getPhone()), SysUser::getPhone, findAllRequest.getPhone())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getIsAccountNonExpired()), SysUser::getIsAccountNonExpired, findAllRequest.getIsAccountNonExpired())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getIsAccountNonLocked()), SysUser::getIsAccountNonLocked, findAllRequest.getIsAccountNonLocked())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getIsCredentialsNonExpired()), SysUser::getIsCredentialsNonExpired,
                        findAllRequest.getIsCredentialsNonExpired())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getIsEnabled()), SysUser::getIsEnabled, findAllRequest.getIsEnabled());
        return repository.selectPage(findAllRequest.getPage(SysUser.class), queryWrapper);
    }

    @Override
    public List<SysUser> findOneByRoleKey(String roleKey) {
        return repository.findOneByRoleKey(roleKey);
    }

    @Override
    public List<SysUser> findOneByRoleId(String roleId) {
        return repository.findOneByRoleId(roleId);
    }

    @Override
    public SysUser findOne(FindOneSysUserRequest findOneRequest, FindOneSysUserRequest neRequest) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(findOneRequest.getId()), SysUser::getId, findOneRequest.getId())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getUsername()), SysUser::getUsername, findOneRequest.getUsername())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getPhone()), SysUser::getPhone, findOneRequest.getPhone())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getPassword()), SysUser::getPassword, findOneRequest.getPassword())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getIsAccountNonExpired()), SysUser::getIsAccountNonExpired, findOneRequest.getIsAccountNonExpired())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getIsAccountNonLocked()), SysUser::getIsAccountNonLocked, findOneRequest.getIsAccountNonLocked())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getIsCredentialsNonExpired()), SysUser::getIsCredentialsNonExpired,
                        findOneRequest.getIsCredentialsNonExpired())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getIsEnabled()), SysUser::getIsEnabled, findOneRequest.getIsEnabled())
                    .ne(ObjUtil.isNotEmpty(neRequest.getId()), SysUser::getId, neRequest.getId())
                    .ne(ObjUtil.isNotEmpty(neRequest.getUsername()), SysUser::getUsername, neRequest.getUsername())
                    .ne(ObjUtil.isNotEmpty(neRequest.getPhone()), SysUser::getPhone, neRequest.getPhone())
                    .ne(ObjUtil.isNotEmpty(neRequest.getPassword()), SysUser::getPassword, neRequest.getPassword())
                    .ne(ObjUtil.isNotEmpty(neRequest.getIsAccountNonExpired()), SysUser::getIsAccountNonExpired, neRequest.getIsAccountNonExpired())
                    .ne(ObjUtil.isNotEmpty(neRequest.getIsAccountNonLocked()), SysUser::getIsAccountNonLocked, neRequest.getIsAccountNonLocked())
                    .ne(ObjUtil.isNotEmpty(neRequest.getIsCredentialsNonExpired()), SysUser::getIsCredentialsNonExpired, neRequest.getIsCredentialsNonExpired())
                    .ne(ObjUtil.isNotEmpty(neRequest.getIsEnabled()), SysUser::getIsEnabled, neRequest.getIsEnabled());

        List<SysUser> list = repository.selectList(queryWrapper);
        if (list.isEmpty()) {
            return null;
        } else if (list.size() > 1) {
            AssertUtils.error("数据异常", "参数为[{}]查询只需要一条数据，但是查询出来多条", JsonUtils.toJsonStr(findOneRequest));
        }
        return list.get(0);
    }

    @Override
    public SysUser findById(String id) {
        return repository.selectById(id);
    }

    @Override
    public void saveOrUpdate(SysUser sysUser) {
        if (sysUser.getId() == null) {
            if (ObjUtil.isEmpty(sysUser.getNickname())) {
                sysUser.setNickname(sysUser.getUsername());
            }
            repository.insert(sysUser);
        } else {
            repository.updateById(sysUser);
        }
    }

    @Override
    public void saves(List<SysUser> list) {
        repository.insertBatch(list);
    }

    @Override
    public void deleteByIds(List<String> ids) {
        repository.deleteByIds(ids);
    }
}

