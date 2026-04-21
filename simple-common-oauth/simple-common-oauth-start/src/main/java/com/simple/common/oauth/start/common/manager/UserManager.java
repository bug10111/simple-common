package com.simple.common.oauth.start.common.manager;

import com.simple.common.oauth.start.common.dto.SysUserByRoleKeyResponse;
import com.simple.common.oauth.start.common.entity.CreateUserRequest;
import com.simple.common.oauth.start.common.entity.RestUserRequest;
import com.simple.common.oauth.start.common.entity.SysUserInfoResponse;
import com.simple.common.oauth.start.common.entity.UpdateUserRequest;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: 用户操作远程调用接口
 * @author qty
 */
public interface UserManager {

    /**
     * 创建用户
     * @param createUserRequest 请求对象
     */
    String create(CreateUserRequest createUserRequest);

    /**
     * 根据id获取用户信息
     * @param id 用户ID
     */
    SysUserInfoResponse findById(String id);

    /**
     * 根据账号获取用户信息
     * @param name 账号
     */
    String findByName(String name);

    /**
     * 获取某角色下的账号
     *
     * @param roleKey 角色id
     */
    List<SysUserByRoleKeyResponse> findOneByRoleKey(String roleKey);

    /**
     * 修改用户信息
     * @param updateSysUserRequest 请求对象
     */
    void update(UpdateUserRequest updateSysUserRequest);

    /**
     * 重置密码
     * @param restUserRequest 请求对象
     */
    void restPwd(RestUserRequest restUserRequest);

    /**
     * 删除用户
     * @param ids id集合
     */
    void delete(List<String> ids);

}
