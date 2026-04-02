package com.simple.oauth.manager.role;

import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.eventbus.common.service.EventBusService;
import com.simple.oauth.common.entity.sysMenu.SysMenu;
import com.simple.oauth.common.entity.sysRole.SysRole;
import com.simple.oauth.common.event.sysMenu.SysMenuUpdatesEvent;
import com.simple.oauth.common.manager.role.RoleAuthCacheManager;
import com.simple.oauth.common.properties.OauthProperties;
import com.simple.oauth.common.view.sysMenu.SysMenuView;
import com.simple.oauth.common.view.sysRole.SysRoleView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Component
public class DefaultRoleAuthCacheManager implements RoleAuthCacheManager {

    @Autowired
    private SysMenuView sysMenuView;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SysRoleView sysRoleView;

    @Autowired
    private OauthProperties oauthProperties;

    @Autowired
    private EventBusService eventBusService;

    @Override
    public void put(String roleKey, List<String> sysMenuIds) {
        List<SysMenu> all = sysMenuView.findAll(sysMenuIds);
        if (ObjUtil.isNotEmpty(all)) {
            putCode(roleKey, all);
        }
    }

    @Override
    public Map<Object, Object> get(String roleKey) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(TokenConstant.getAuthKey(roleKey));
        if (ObjUtil.isEmpty(entries)) {
            synchronized (this) {
                entries = stringRedisTemplate.opsForHash().entries(TokenConstant.getAuthKey(roleKey));
                if (ObjUtil.isEmpty(entries)) {
                    List<SysMenu> allByRoleKey;
                    if (roleKey.equals(oauthProperties.getAdmin())) {
                        allByRoleKey = sysMenuView.findAllByRoleKey(null);
                    } else {
                        allByRoleKey = sysMenuView.findAllByRoleKey(roleKey);
                    }
                    if (ObjUtil.isNotEmpty(allByRoleKey)) {
                        return putCode(roleKey, allByRoleKey);
                    }
                }
            }
        }
        return entries;
    }

    @Override
    public void update() {
        List<SysRole> all = sysRoleView.findAll();
        all.forEach(sysRole -> {
            List<SysMenu> allByRoleKey = sysMenuView.findAllByRoleKey(sysRole.getRoleKey());
            putCode(sysRole.getRoleKey(), allByRoleKey);
        });
    }

    /**
     * 缓存权限信息
     *
     * @param roleKey 角色code
     * @param all     权限信息
     */
    protected Map<Object, Object> putCode(String roleKey, List<SysMenu> all) {
        Map<Object, Object> collect = all.stream().filter(Objects::nonNull).collect(Collectors.toMap(SysMenu::getPerms, SysMenu::getPerms, (existing, replacement) -> replacement));

        if(ObjUtil.isEmpty(collect)) {
            return null;
        }

        //先清空
        stringRedisTemplate.delete(TokenConstant.getAuthKey(roleKey));

        //在缓存
        stringRedisTemplate.opsForHash().putAll(TokenConstant.getAuthKey(roleKey), collect);

        //更新所有客户端
        if(oauthProperties.getMenuOpenAll()){
            SysMenuUpdatesEvent event = new SysMenuUpdatesEvent();
            event.setAuthKey(TokenConstant.getAuthKey(roleKey));
            event.setCollect(collect);
            eventBusService.push(event);
        }
        return collect;
    }

}
