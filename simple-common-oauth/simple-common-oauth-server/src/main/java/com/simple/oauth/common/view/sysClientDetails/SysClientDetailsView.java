package com.simple.oauth.common.view.sysClientDetails;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.CryptoUtil;
import com.simple.oauth.common.dto.sysClientDetails.FindAllSysClientDetailsRequest;
import com.simple.oauth.common.entity.sysClientDetails.SysClientDetails;
import com.simple.oauth.common.enums.ServerType;

import java.util.List;

/**
 * 客户端信息(sys_client_details)数据库视图接口
 *
 * @author qty
 */
public interface SysClientDetailsView {

    /**
     * 分页列表
     *
     * @param findAllRequest 分页参数
     * @return 分页数据
     */
    IPage<SysClientDetails> findAll(FindAllSysClientDetailsRequest findAllRequest);

    /**
     * 获取列表
     *
     * @param server     服务
     * @param serverType 服务类型
     */
    List<SysClientDetails> list(String server, ServerType serverType);

    /**
     * 获取所有客户端信息
     *
     * @return 分页数据
     */
    List<SysClientDetails> findAll(ServerType serverType);

    /**
     * 获取所有客户端信息
     *
     * @return 分页数据
     */
    SysClientDetails findAllByClientId(String clientId);

    /**
     * 根据主键获取数据
     *
     * @param id         主键
     * @param allowEmpty 允许空值返回
     * @return SysClientDetails 原始表数据
     */
    SysClientDetails findById(String id, Boolean allowEmpty);

    /**
     * 获取数据
     *
     * @param name 客户端名称
     * @param id   客户端名称
     * @return SysClientDetails 原始表数据
     */
    SysClientDetails findByNameAndNeqId(String name, String id);

    /**
     * 获取数据
     *
     * @param clientId 客户端id
     * @param id       客户端id
     * @return SysClientDetails 原始表数据
     */
    SysClientDetails findByClientIdAndNeqId(String clientId, String id);

    /**
     * 获取数据
     *
     * @param clientId clientId
     */
    SysClientDetails findByClientId(String clientId);

    /**
     * 根据ID获取客户端信息
     *
     * @param id 客户端ID
     * @return 客户端信息
     */
    default SysClientDetails getById(String id) {
        return findById(id, false);
    }

    /**
     * 根据客户端ID获取客户端信息
     *
     * @param clientId 客户端ID
     * @return 客户端信息
     */
    default SysClientDetails getByClientId(String clientId) {
        return findByClientId(clientId);
    }

    /**
     * 更新客户端信息
     *
     * @param sysClientDetails 客户端信息
     */
    default void updateById(SysClientDetails sysClientDetails) {
        save(sysClientDetails);
    }

    /**
     * 根据ID删除客户端信息
     *
     * @param id 客户端ID
     */
    default void deleteById(String id) {
        deleteByIds(List.of(id));
    }

    /**
     * 获取客户端列表
     *
     * @param appName 应用名称
     * @return 客户端列表
     */
    default List<SysClientDetails> list(String appName) {
        return list(appName, null);
    }

    /**
     * 新增,或者根据id修改
     *
     * @param sysClientDetails 客户端信息对象
     */
    void save(SysClientDetails sysClientDetails);

    /**
     * 批量新增
     *
     * @param list 对象
     */
    void saves(List<SysClientDetails> list);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);

    /**
     * 添加新的客户端，并加载对应密钥
     *
     * @param sysClientDetails 客户端信息对象
     */
    default void saveAndLoading(SysClientDetails sysClientDetails) {
        loading(sysClientDetails);
        if (ObjUtil.isNotEmpty(sysClientDetails.getClientSecret())) {
            sysClientDetails.setClientSecret(CryptoUtil.hashPassword(sysClientDetails.getClientSecret()));
        }
        save(sysClientDetails);
    }

    /**
     * 加载密钥
     *
     * @param sysClientDetails 客户端对象
     */
    default void loading(SysClientDetails sysClientDetails) {
        if(sysClientDetails.getServerType() == ServerType.client){
            // RSA密钥已存储到数据库，由客户端自行获取
            // AES密钥也已存储到数据库
        }

    }

}