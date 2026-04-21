package com.simple.oauth.common.service.sysUser;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.oauth.common.dto.sysUser.*;
import com.simple.oauth.common.dto.wxLogin.WechatInfoDTO;
import com.simple.oauth.common.entity.sysUser.SysUser;

import java.util.List;

/**
 * 用户(sys_user)接口
 *
 * @author qty
 */
public interface SysUserService {

    /**
     * 分页列表
     *
     * @param findAllRequest 请求参数
     * @return 分页数据
     */
    IPage<SysUserPageResponse> findAll(FindAllSysUserRequest findAllRequest);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysUserFullInfoResponse  用户 详细数据
     */
    SysUserInfoResponse findById(String id);

    /**
     * 获取单条数据
     *
     * @param name 账号
     * @return SysUserFullInfoResponse  用户 详细数据
     */
    String findByName(String name);

    /**
     * 获取某角色下的账号
     *
     * @param roleKey 角色id
     */
    List<SysUserByRoleKeyResponse> findOneByRoleKey(String roleKey);

    /**
     * 获取某角色下的账号
     *
     * @param roleId 角色id
     */
    List<SysUserByRoleKeyResponse> findOneByRoleId(String roleId);

    /**
     * 新增
     *
     * @param createRequest 用户 请求对象
     */
    String save(CreateSysUserRequest createRequest);

    /**
     * 注册微信登录用户
     *
     * @param wechatInfoDTO 微信登录返回字段
     * @param clientDetails 客户端信息
     */
    SysUser save(WechatInfoDTO wechatInfoDTO, ClientDetails clientDetails);

    /**
     * 用户绑定角色
     *
     * @param userId        用户ID
     * @param createRequest 请求对象
     */
    void bindingRole(String userId, List<BindingRoleRequest> createRequest);

    /**
     * 绑定手机号
     *
     * @param request 参数对象
     */
    void bindingPhone(BindingPhoneRequest request);

    /**
     * 根据主键修改
     *
     * @param updateRequest 用户 请求对象
     */
    String updateById(UpdateSysUserRequest updateRequest);

    /**
     * 重置密码
     *
     * @param resetPwdRequest 请求对象
     */
    void resetPwd(RestSysUserRequest resetPwdRequest);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);

    /***
     * 解除绑定账号
     */
    void disarmAccount();

    /**
     * 绑定账号
     *
     * @param request 参数
     */
    void bindingAccount(BindingAccountRequest request);
}

