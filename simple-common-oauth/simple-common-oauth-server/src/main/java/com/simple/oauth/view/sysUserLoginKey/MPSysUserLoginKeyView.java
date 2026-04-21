package com.simple.oauth.view.sysUserLoginKey;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.oauth.common.dto.sysUserLoginKey.FindAllSysUserLoginKeyRequest;
import com.simple.oauth.common.dto.sysUserLoginKey.FindOneSysUserLoginKeyRequest;
import com.simple.oauth.common.entity.sysUserLoginKey.SysUserLoginKey;
import com.simple.oauth.common.view.sysUserLoginKey.SysUserLoginKeyView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户登录标志(sys_user_login_key)数据库视图实现
 *
 * @author qty
 */
@Component
class MPSysUserLoginKeyView implements SysUserLoginKeyView {

    @Autowired
    private SysUserLoginKeyRepository repository;

    @Override
    public IPage<SysUserLoginKey> findAll(FindAllSysUserLoginKeyRequest findAllRequest) {
        LambdaQueryWrapper<SysUserLoginKey> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(ObjUtil.isNotEmpty(findAllRequest.getUserId()), SysUserLoginKey::getUserId, findAllRequest.getUserId())
                    .likeRight(ObjUtil.isNotEmpty(findAllRequest.getLoginKey()), SysUserLoginKey::getLoginKey, findAllRequest.getLoginKey());
        return repository.selectPage(findAllRequest.getPage(SysUserLoginKey.class), queryWrapper);
    }

    @Override
    public SysUserLoginKey findOne(FindOneSysUserLoginKeyRequest findOneRequest, FindOneSysUserLoginKeyRequest neRequest) {
        LambdaQueryWrapper<SysUserLoginKey> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(findOneRequest.getId()), SysUserLoginKey::getId, findOneRequest.getId())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getUserId()), SysUserLoginKey::getUserId, findOneRequest.getUserId())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getLoginKey()), SysUserLoginKey::getLoginKey, findOneRequest.getLoginKey())
                    .ne(ObjUtil.isNotEmpty(neRequest.getId()), SysUserLoginKey::getId, neRequest.getId())
                    .ne(ObjUtil.isNotEmpty(neRequest.getUserId()), SysUserLoginKey::getUserId, neRequest.getUserId());

        List<SysUserLoginKey> list = repository.selectList(queryWrapper);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public SysUserLoginKey findById(String id) {
        return repository.selectById(id);
    }

    @Override
    public void save(SysUserLoginKey sysUserLoginKey) {
        repository.insert(sysUserLoginKey);
    }

    @Override
    public void updateById(SysUserLoginKey sysUserLoginKey) {
        repository.updateById(sysUserLoginKey);
    }

    @Override
    public void saves(List<SysUserLoginKey> list) {
        repository.insertBatch(list);
    }

    @Override
    public void deleteByIds(List<String> ids) {
        repository.deleteByIds(ids);
    }

    @Override
    public void deleteByUserId(String userId) {
        repository.delete(new LambdaQueryWrapper<SysUserLoginKey>().eq(SysUserLoginKey::getUserId, userId));
    }

}

