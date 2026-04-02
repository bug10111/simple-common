package com.simple.oauth.common.view.sysRole;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.oauth.common.dto.sysRole.FindAllSysRoleRequest;
import com.simple.oauth.common.dto.sysRole.FindOneSysRoleRequest;
import com.simple.oauth.common.dto.sysRole.SysRoleInfoResponse;
import com.simple.oauth.common.entity.sysRole.SysRole;

import java.util.List;

/**
 * 角色信息(sys_role)数据库视图接口
 *
 * @author 兄台丶请冷静
 */
public interface SysRoleView {

    /**
     * 分页列表
     *
     * @param findAllRequest 分页参数
     * @return 分页数据
     */
    IPage<SysRole> findAll(FindAllSysRoleRequest findAllRequest);

    /**
     * 获取所有角色
     *
     */
    List<SysRole> findAll();

    /**
     * 获取角色列表
     *
     * @param userId 用户ID
     */
    List<SysRoleInfoResponse> findByUserIdAndServer(String userId,String server);


    /**
     * 获取单条数据
     *
     * @param id         主键
     * @param allowEmpty 允许空值返回
     * @return SysRole 原始表数据
     */
    SysRole findById(String id, Boolean allowEmpty);

    /**
     * 获取单条数据
     *
     * @param findOneRequest 查询条件
     * @param neRequest      排除条件
     * @param allowEmpty     允许空值返回
     * @return SysRole 原始表数据
     */
    SysRole findOne(FindOneSysRoleRequest findOneRequest, FindOneSysRoleRequest neRequest, Boolean allowEmpty);

    /**
     * 新增,或者根据id修改
     *
     * @param sysRole 角色信息对象
     */
    void saveOrUpdate(SysRole sysRole);

    /**
     * 批量新增
     *
     * @param list 对象
     */
    void saves(List<SysRole> list);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);

    /**
     * 获取单条数据
     *
     * @param findOneRequest 查询条件
     * @param allowEmpty     允许空值返回
     * @return SysRole 原始表数据
     */
    default SysRole findOne(FindOneSysRoleRequest findOneRequest, Boolean allowEmpty) {
        return findOne(findOneRequest, new FindOneSysRoleRequest(), allowEmpty);
    }

    /**
     * 获取角色列表
     *
     * @param userId 用户ID
     */
    default List<SysRoleInfoResponse> findByUserIdAndServer(String userId){
        return findByUserIdAndServer(userId,null);
    }

}

