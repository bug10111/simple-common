package com.simple.common.oauth.start.service;

import com.simple.common.oauth.start.common.dto.SysRoleInfoResponse;
import com.simple.common.oauth.start.common.manager.RoleManager;
import com.simple.common.oauth.start.common.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Service
public class DefaultRoleService implements RoleService {

    @Autowired
    private RoleManager roleManager;

    @Override
    public SysRoleInfoResponse findById(String id) {
        return roleManager.findById(id);
    }

}
