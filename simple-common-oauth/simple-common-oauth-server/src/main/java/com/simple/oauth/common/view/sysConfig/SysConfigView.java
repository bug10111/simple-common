package com.simple.oauth.common.view.sysConfig;

import com.simple.oauth.common.entity.sysConfig.SysConfig;

/**
 * 系统配置(sys_config)数据库视图接口
 *
 * @author qty
 */
public interface SysConfigView {

    /**
     * 根据主键获取数据
     *
     * @param id         主键
     * @param allowEmpty 允许空值返回
     * @return SysConfig 原始表数据
     */
    SysConfig findById(String id, Boolean allowEmpty);

    /**
     * 新增,或者根据id修改
     *
     * @param sysConfig 系统配置对象
     */
    void save(SysConfig sysConfig);

}

