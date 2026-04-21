package com.simple.common.oauth.start.common.service;

import com.simple.common.oauth.start.common.dto.SysRoleInfoResponse;

/**
 * Created with IntelliJ IDEA
 * Description: 角色相关接口
 *
 * @author qty
 */
public interface RoleService {

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysRoleFullInfoResponse  角色信息 详细数据
     */
    SysRoleInfoResponse findById(String id);
}
