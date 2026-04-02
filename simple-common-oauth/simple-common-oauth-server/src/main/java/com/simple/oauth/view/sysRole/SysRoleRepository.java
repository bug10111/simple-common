package com.simple.oauth.view.sysRole;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simple.oauth.common.dto.sysRole.SysRoleInfoResponse;
import com.simple.oauth.common.entity.sysRole.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色信息(sys_role)数据库访问层
 *
 * @author 兄台丶请冷静
 */
@Mapper
public interface SysRoleRepository extends BaseMapper<SysRole> {

    /**
     * 获取角色列表
     *
     * @param userId
     * @param server
     * @return
     */
    List<SysRoleInfoResponse> getRole(@Param("userId") String userId,@Param("server") String server);

    /**
     * 批量新增数据（MyBatis原生foreach方法，MP表的自动化操作都无效，需要手动为集合对象赋值）
     *
     * @param entities List<SysRole> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<SysRole> entities);

}

