package com.simple.oauth.common.service.sysClientDetails;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.oauth.common.dto.api.ApiSysClientDetailsResponse;
import com.simple.oauth.common.dto.sysClientDetails.*;

import java.util.List;
import java.util.Map;

/**
 * 客户端信息(sys_client_details)接口
 *
 * @author 兄台丶请冷静
 */
public interface SysClientDetailsService {

    /**
     * 分页列表
     *
     * @param findAllRequest 请求参数
     * @return 分页数据
     */
    IPage<SysClientDetailsPageResponse> findAll(FindAllSysClientDetailsRequest findAllRequest);

    /**
     * 获取客户端列表
     *
     * @param server 服务
     */
    List<ApiSysClientDetailsResponse> list(String server);

    /**
     * 根据主键获取数据
     *
     * @param id 主键
     * @return SysClientDetailsFullInfoResponse  客户端信息 详细数据
     */
    SysClientDetailsInfoResponse findById(String id);

    /**
     * 新增
     *
     * @param createRequest 客户端信息 请求对象
     */
    String save(CreateSysClientDetailsRequest createRequest);

    /**
     * 根据主键修改
     *
     * @param createRequest 客户端信息 请求对象
     */
    void updateById(UpdateSysClientDetailsRequest createRequest);

    /**
     * 根据主键修改
     *
     * @param createRequest 客户端信息 请求对象
     */
    void resetPwd(RestSysClientRequest createRequest);

    /**
     * 创建密钥信息
     */
    Map<String, Object> createKey();

    /**
     * 更新客户端秘钥
     *
     * @param clientId 客户端ID
     * @param hsKey    HMAC秘钥（可选）
     * @param rsaPublic RSA公钥（可选）
     * @param rsaPrivate RSA私钥（可选）
     */
    void updateSecret(String clientId, String hsKey, String rsaPublic, String rsaPrivate);

    /**
     * 重新生成客户端秘钥
     *
     * @param clientId 客户端ID
     * @param secretTypes 秘钥类型数组（hsKey, rsaPublic, rsaPrivate）
     * @return 新生成的秘钥信息
     */
    Map<String, Object> regenerateSecret(String clientId, String[] secretTypes);

    /**
     * 删除客户端
     *
     * @param id 客户端ID
     */
    void deleteById(String id);

    /**
     * 根据主键批量删除
     *
     * @param ids 主键列表
     */
    void deleteByIds(List<String> ids);
}