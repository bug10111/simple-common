package com.simple.common.oauth.start.common.manager;

import com.simple.common.oauth.start.common.dto.SysRoleInfoResponse;

/**
 * Created with IntelliJ IDEA
 * Description: 角色远程调用相关
 *
 * @author qty
 */
public interface RoleManager {

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysRoleFullInfoResponse  角色信息 详细数据
     */
    SysRoleInfoResponse findById(String id);
}
