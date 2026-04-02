package com.simple.oauth.view.sysRoleMenu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.simple.common.core.utils.AssertUtils;
import com.simple.oauth.common.entity.sysRoleMenu.SysRoleMenu;
import com.simple.oauth.common.view.sysRoleMenu.SysRoleMenuView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 角色和菜单关联(sys_role_menu)数据库视图实现
 *
 * @author 兄台丶请冷静
 */
@Component
class MPSysRoleMenuView implements SysRoleMenuView {

    @Autowired
    private SysRoleMenuRepository repository;

    @Override
    public SysRoleMenu findById(String id, Boolean allowEmpty) {
        SysRoleMenu sysRoleMenu = repository.selectById(id);
        if (!allowEmpty && sysRoleMenu == null) {
            AssertUtils.errorParams("主键为[{}]的数据为空", id);
        }
        return sysRoleMenu;
    }

    @Override
    public void saveOrUpdate(SysRoleMenu sysRoleMenu) {
        if (sysRoleMenu.getId() == null) {
            repository.insert(sysRoleMenu);
        } else {
            repository.updateById(sysRoleMenu);
        }
    }

    @Override
    public void saves(List<SysRoleMenu> list) {
        repository.insertBatch(list);
    }

    @Override
    public void deleteByRoleId(String roleId) {
        repository.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
    }

    @Override
    public void deleteByMenuId(String menuId) {
        repository.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, menuId));
    }

    @Override
    public void deleteByIds(List<String> ids) {
        repository.deleteByIds(ids);
    }
}

