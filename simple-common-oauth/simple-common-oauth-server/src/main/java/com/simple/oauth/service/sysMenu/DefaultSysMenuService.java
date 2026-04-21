package com.simple.oauth.service.sysMenu;

import com.simple.common.core.utils.BeanUtils;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.RecursiveUtils;
import com.simple.common.eventbus.common.service.EventBusService;
import com.simple.oauth.common.dto.sysMenu.CreateSysMenuRequest;
import com.simple.oauth.common.dto.sysMenu.SysMenuInfoResponse;
import com.simple.oauth.common.dto.sysMenu.SysMenuPageResponse;
import com.simple.oauth.common.dto.sysMenu.UpdateSysMenuRequest;
import com.simple.oauth.common.entity.sysMenu.SysMenu;
import com.simple.oauth.common.entity.sysRole.SysRole;
import com.simple.oauth.common.event.sysMenu.SysMenuCreatedEvent;
import com.simple.oauth.common.event.sysMenu.SysMenuDeletedEvent;
import com.simple.oauth.common.properties.OauthProperties;
import com.simple.oauth.common.service.sysMenu.SysMenuService;
import com.simple.oauth.common.view.sysMenu.SysMenuView;
import com.simple.oauth.common.view.sysRole.SysRoleView;
import com.simple.oauth.common.view.sysRoleMenu.SysRoleMenuView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单权限(sys_menu)默认接口实现
 *
 * @author qty
 */
@Service
@Transactional
class DefaultSysMenuService implements SysMenuService {

    @Autowired
    private SysMenuView sysMenuView;

    @Autowired
    private SysRoleView sysRoleView;

    @Autowired
    private SysRoleMenuView sysRoleMenuView;

    @Autowired
    private OauthProperties oauthProperties;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private EventBusService eventBusService;

    @Override
    public List<Tree<String>> findAll(String clientId) {
        var listAll = sysMenuView.findAll(clientId);
        List<SysMenuPageResponse> listResponse = listAll.stream().map(sysMenu -> BeanUtils.copyProperties(sysMenu, SysMenuPageResponse.class)).toList();
        return RecursiveUtils.get(listResponse);
    }

    @Override
    public Set<SysMenuPageResponse> findAllByLoginUser(Set<String> loginRole, String userId, String clientId) {
        List<SysMenu> allByUserId;
        if (loginRole.contains(oauthProperties.getAdmin())) {
            allByUserId = sysMenuView.findAll(clientId);
        } else {
            allByUserId = sysMenuView.findAllByUserId(userId,clientId);
        }

        return allByUserId.stream().filter(ObjUtil::isNotEmpty)
                          .map(sysMenu -> BeanUtils.copyProperties(sysMenu, SysMenuPageResponse.class))
                          .collect(Collectors.toSet());
    }

    @Override
    public List<Tree<String>> findAllByRoleId(String roleId, String clientId) {
        SysRole role = sysRoleView.findById(roleId, false);
        List<SysMenu> listAll;
        if (oauthProperties.getAdmin().equals(role.getRoleKey())) {
            listAll = sysMenuView.findAll("");
        } else {
            listAll = sysMenuView.findAllByRoleId(roleId,clientId);
        }
        List<SysMenuPageResponse> listResponse = listAll.stream().map(sysMenu -> BeanUtils.copyProperties(sysMenu, SysMenuPageResponse.class)).toList();
        return RecursiveUtils.get(listResponse);
    }

    @Override
    public SysMenuInfoResponse findById(String id) {
        var sysMenu = sysMenuView.findById(id, false);
        return BeanUtils.copyProperties(sysMenu, SysMenuInfoResponse.class);
    }

    @Override
    public String save(CreateSysMenuRequest createRequest) {

        //添加的第一级自动追加父ID
        if (ObjUtil.isEmpty(createRequest.getParentId())) {
            createRequest.setParentId(RecursiveUtils.initial_id);
        }

        var sysMenu = BeanUtils.copyProperties(createRequest, SysMenu.class);
        SysMenu byCode = sysMenuView.findByCode(createRequest.getCode());
        AssertUtils.isTrueParams(byCode == null, "code[{}]已存在", createRequest.getCode());

        SysMenu byPerms = sysMenuView.findByPerms(createRequest.getPerms());
        AssertUtils.isTrueParams(byPerms == null, "perms[{}]已存在", createRequest.getPerms());

        sysMenuView.saveOrUpdate(sysMenu);

        //事件发布
        SysMenuCreatedEvent event = BeanUtils.copyProperties(sysMenu, SysMenuCreatedEvent.class);
        event.setAuthKey(TokenConstant.getAuthKey(oauthProperties.getAdmin()));
        eventBusService.push(event);
        return sysMenu.getId();
    }

    @Override
    public void updateById(UpdateSysMenuRequest createRequest) {
        var sysMenu = BeanUtils.copyProperties(createRequest, SysMenu.class);
        sysMenuView.findById(sysMenu.getId(), false);

//        SysMenu byCode = sysMenuView.findByCodeAndNeId(createRequest.getCode(), createRequest.getId());
//        AssertUtils.isTrue(byCode == null, "code已存在");
//
//        SysMenu byPermsAndNeId = sysMenuView.findByPermsAndNeId(createRequest.getPerms(), createRequest.getId());
//        AssertUtils.isTrue(byPermsAndNeId == null, "code已存在");
        sysMenuView.saveOrUpdate(sysMenu);

        //事件发布
        SysMenuCreatedEvent event = BeanUtils.copyProperties(sysMenu, SysMenuCreatedEvent.class);
        event.setAuthKey(TokenConstant.getAuthKey(oauthProperties.getAdmin()));
        eventBusService.push(event);
    }

    @Transactional
    @Override
    public void deleteByIds(List<String> ids) {
        ids.forEach(s -> {
            sysRoleMenuView.deleteByMenuId(s);
            SysMenu byId = sysMenuView.findById(s, true);
            if(ObjUtil.isNotEmpty(byId)) {
                SysMenuDeletedEvent event = BeanUtils.copyProperties(byId, SysMenuDeletedEvent.class);
                event.setAuthKey(TokenConstant.getAuthKey(oauthProperties.getAdmin()));
                eventBusService.push(event);
            }
        });
        sysMenuView.deleteByIds(ids);
    }
}

