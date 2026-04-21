package com.simple.oauth.common.service.sysRole;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.oauth.common.dto.sysRole.*;

import java.util.List;

/**
 * 角色信息(sys_role)接口
 *
 * @author qty
 */
public interface SysRoleService {

    /**
     * 分页列表
     *
     * @param findAllRequest 请求参数
     * @return 分页数据
     */
    IPage<SysRolePageResponse> findAll(FindAllSysRoleRequest findAllRequest);

    /**
     * 分页列表
     *
     * @param findAllRequest 请求参数
     * @return 分页数据
     */
    List<SysRolePageResponse> listbyUser(FindAllSysRoleRequest findAllRequest);

    /**
     * 获取角色列表
     *
     * @param userId 用户ID
     */
    List<SysRoleInfoResponse> getRole(String userId);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysRoleFullInfoResponse  角色信息 详细数据
     */
    SysRoleInfoResponse findById(String id);

    /**
     * 新增
     *
     * @param createRequest 角色信息 请求对象
     */
    String save(CreateSysRoleRequest createRequest);

    /**
     * 根据主键修改
     *
     * @param updateRequest 角色信息 请求对象
     */
    String updateById(UpdateSysRoleRequest updateRequest);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);

}

