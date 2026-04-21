package com.simple.oauth.view.sysUserLoginKey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simple.oauth.common.entity.sysUserLoginKey.SysUserLoginKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户登录标志(sys_user_login_key)数据库访问层
 *
 * @author qty
 */
@Mapper
public interface SysUserLoginKeyRepository extends BaseMapper<SysUserLoginKey> {

    /**
     * 批量新增数据（MyBatis原生foreach方法，MP表的自动化操作都无效，需要手动为集合对象赋值）
     *
     * @param entities List<SysUserLoginKey> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<SysUserLoginKey> entities);

}

