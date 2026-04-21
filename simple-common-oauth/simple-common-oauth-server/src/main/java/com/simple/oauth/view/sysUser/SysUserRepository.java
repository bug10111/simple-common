package com.simple.oauth.view.sysUser;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simple.oauth.common.entity.sysUser.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户(sys_user)数据库访问层
 *
 * @author qty
 */
@Mapper
public interface SysUserRepository extends BaseMapper<SysUser> {

    /**
     * 批量新增数据（MyBatis原生foreach方法，MP表的自动化操作都无效，需要手动为集合对象赋值）
     *
     * @param entities List<SysUser> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<SysUser> entities);

    /**
     * 根据角色获取用户列表
     * @param roleKey roleKey
     */
    List<SysUser> findOneByRoleKey(@Param("roleKey") String roleKey);
    List<SysUser> findOneByRoleId(@Param("roleId") String roleId);
}

