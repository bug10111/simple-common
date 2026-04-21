package com.simple.common.oauth.start.service;

import com.simple.common.oauth.start.common.dto.SysUserByRoleKeyResponse;
import com.simple.common.oauth.start.common.entity.CreateUserRequest;
import com.simple.common.oauth.start.common.entity.RestUserRequest;
import com.simple.common.oauth.start.common.entity.SysUserInfoResponse;
import com.simple.common.oauth.start.common.entity.UpdateUserRequest;
import com.simple.common.oauth.start.common.manager.UserManager;
import com.simple.common.oauth.start.common.properties.OauthStartProperties;
import com.simple.common.oauth.start.common.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Service
public class DefaultUserService implements UserService {

    @Autowired
    private UserManager userManager;

    @Autowired
    private OauthStartProperties oauthStartProperties;

    @Override
    public String create(CreateUserRequest createUserRequest) {
        createUserRequest.setPasswordNew(oauthStartProperties.getDefaultPwd());
        return userManager.create(createUserRequest);
    }

    @Override
    public SysUserInfoResponse findById(String id) {
        return userManager.findById(id);
    }

    @Override
    public String findByName(String name) {
        return userManager.findByName(name);
    }

    @Override
    public List<SysUserByRoleKeyResponse> findOneByRoleKey(String roleKey) {
        return userManager.findOneByRoleKey(roleKey);
    }

    @Override
    public void update(UpdateUserRequest updateSysUserRequest) {
        userManager.update(updateSysUserRequest);
    }

    @Override
    public void restPwd(RestUserRequest restUserRequest) {
        restUserRequest.setPassword(oauthStartProperties.getDefaultPwd());
        userManager.restPwd(restUserRequest);
    }

    @Override
    public void delete(List<String> ids) {
        userManager.delete(ids);
    }
}
