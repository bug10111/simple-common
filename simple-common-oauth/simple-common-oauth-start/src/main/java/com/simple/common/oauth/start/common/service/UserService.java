package com.simple.common.oauth.start.common.service;

import com.simple.common.oauth.start.common.dto.SysUserByRoleKeyResponse;
import com.simple.common.oauth.start.common.entity.CreateUserRequest;
import com.simple.common.oauth.start.common.entity.RestUserRequest;
import com.simple.common.oauth.start.common.entity.SysUserInfoResponse;
import com.simple.common.oauth.start.common.entity.UpdateUserRequest;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: 用户操作
 * @author 兄台丶请冷静
 */
public interface UserService {

    /**
     * 创建用户
     * @param createUserRequest 请求对象
     */
    String create(@Validated CreateUserRequest createUserRequest);

    /**
     * 根据id获取用户信息
     * @param id 用户ID
     */
    SysUserInfoResponse findById(@NotNull String id);

    /**
     * 根据账号获取用户信息
     *
     * @param name 账号
     */
    String findByName(@NotNull String name);

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
    void update(@Validated UpdateUserRequest updateSysUserRequest);

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
