package com.simple.oauth.view.sysMenu;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.simple.common.core.utils.AssertUtils;
import com.simple.oauth.common.entity.sysMenu.SysMenu;
import com.simple.oauth.common.view.sysMenu.SysMenuView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 菜单权限(sys_menu)数据库视图实现
 *
 * @author 兄台丶请冷静
 */
@Component
class MPSysMenuView implements SysMenuView {

    @Autowired
    private SysMenuRepository repository;

    @Override
    public List<SysMenu> findAll(String clientId) {
        return repository.selectList(new LambdaQueryWrapper<SysMenu>().eq(ObjUtil.isNotEmpty(clientId), SysMenu::getClientId, clientId));
    }

    @Override
    public List<SysMenu> findAll(List<String> ids) {
        return repository.selectList(new LambdaQueryWrapper<SysMenu>().in(SysMenu::getId, ids).select(SysMenu::getPerms));
    }

    @Override
    public List<SysMenu> findAllByRoleId(String roleId, String clientId) {
        return repository.findAllByRoleId(roleId, clientId);
    }

    @Override
    public List<SysMenu> findAllByRoleKey(String roleKey) {
        return repository.findAllByRoleKey(roleKey);
    }

    @Override
    public List<SysMenu> findAllByUserId(String userId, String clientId) {
        return repository.findAllByUserId(userId, clientId);
    }

    @Override
    public SysMenu findById(String id, Boolean allowEmpty) {
        SysMenu sysMenu = repository.selectById(id);
        if (!allowEmpty && sysMenu == null) {
            AssertUtils.errorParams("主键为[{}]的数据为空", id);
        }
        return sysMenu;
    }

    @Override
    public SysMenu findByCode(String code) {
        return repository.selectOne(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getCode, code));
    }

    @Override
    public SysMenu findByPerms(String perms) {
        return repository.selectOne(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getPerms, perms));
    }

    @Override
    public SysMenu findByCodeAndNeId(String code, String id) {
        return repository.selectOne(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getCode, code).ne(SysMenu::getId, id));
    }

    @Override
    public SysMenu findByPermsAndNeId(String perms, String id) {
        return repository.selectOne(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getPerms, perms).ne(SysMenu::getId, id));
    }

    @Override
    public void saveOrUpdate(SysMenu sysMenu) {
        if (sysMenu.getId() == null) {
            repository.insert(sysMenu);
        } else {
            repository.updateById(sysMenu);
        }
    }

    @Override
    public void saves(List<SysMenu> list) {
        repository.insertBatch(list);
    }

    @Override
    public void deleteByIds(List<String> ids) {
        repository.deleteByIds(ids);
    }
}

