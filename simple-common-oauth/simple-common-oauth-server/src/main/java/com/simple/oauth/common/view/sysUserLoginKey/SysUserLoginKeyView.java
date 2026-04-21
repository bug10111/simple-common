package com.simple.oauth.common.view.sysUserLoginKey;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.oauth.common.dto.sysUserLoginKey.FindAllSysUserLoginKeyRequest;
import com.simple.oauth.common.dto.sysUserLoginKey.FindOneSysUserLoginKeyRequest;
import com.simple.oauth.common.entity.sysUserLoginKey.SysUserLoginKey;

import java.util.List;

/**
 * 用户登录标志(sys_user_login_key)数据库视图接口
 *
 * @author qty
 */
public interface SysUserLoginKeyView {

    /**
     * 分页列表
     *
     * @param findAllRequest 分页参数
     * @return 分页数据
     */
    IPage<SysUserLoginKey> findAll(FindAllSysUserLoginKeyRequest findAllRequest);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysUserLoginKey 原始表数据
     */
    SysUserLoginKey findById(String id);

    /**
     * 获取单条数据
     *
     * @param findOneRequest 查询条件
     * @param neRequest      排除条件
     * @return SysUserLoginKey 原始表数据
     */
    SysUserLoginKey findOne(FindOneSysUserLoginKeyRequest findOneRequest, FindOneSysUserLoginKeyRequest neRequest);

    /**
     * 新增
     *
     * @param sysUserLoginKey 用户登录标志对象
     */
    void save(SysUserLoginKey sysUserLoginKey);

    /**
     * 根据id修改
     *
     * @param sysUserLoginKey 用户登录标志对象
     */
    void updateById(SysUserLoginKey sysUserLoginKey);

    /**
     * 批量新增
     *
     * @param list 对象
     */
    void saves(List<SysUserLoginKey> list);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);

    /**
     * 删除
     *
     * @param userId 用户Id
     */
    void deleteByUserId(String userId);

    /**
     * 获取单条数据
     *
     * @param findOneRequest 查询条件
     * @return SysUserLoginKey 原始表数据
     */
    default SysUserLoginKey findOne(FindOneSysUserLoginKeyRequest findOneRequest) {
        return findOne(findOneRequest, new FindOneSysUserLoginKeyRequest());
    }

    /**
     * 获取单条数据
     *
     * @param findOneRequest 查询条件
     * @return SysUserLoginKey 原始表数据
     */
    default SysUserLoginKey findOne(FindOneSysUserLoginKeyRequest findOneRequest, String clientId) {
        findOneRequest.setLoginKey(getLoginKey(findOneRequest.getLoginKey(), clientId));
        return findOne(findOneRequest);
    }

    /**
     * 新增
     *
     * @param sysUserLoginKey 用户登录标志对象
     */
    default void save(SysUserLoginKey sysUserLoginKey, String clientId) {
        sysUserLoginKey.setLoginKey(getLoginKey(sysUserLoginKey.getLoginKey(), clientId));
        save(sysUserLoginKey);
    }

    /**
     * 获取保存时候的Login key，这是为了防止不同第三方登录标志重复
     *
     * @param loginKey 登录标志
     * @param client   客户端Id
     */
    default String getLoginKey(String loginKey, String client) {
        return loginKey + "&&" + client;
    }

}

