package com.simple.oauth.common.service.sysAnnex;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.annex.common.enums.ShareType;
import com.simple.oauth.common.dto.sysAnnex.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 附件(sys_annex)接口
 *
 * @author 兄台丶请冷静
 */
public interface SysAnnexService {

    /**
     * 分页列表
     *
     * @param findAllRequest 请求参数
     * @return 分页数据
     */
    IPage<SysAnnexPageResponse> findAll(FindAllSysAnnexRequest findAllRequest);

    /**
     * 获取单条数据
     *
     * @param id 主键
     */
    String get(String id);

    /**
     * 获取多数据
     *
     * @param ids 主键
     */
    List<AnnexListResponse> get(List<String> ids);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysAnnexFullInfoResponse  附件 详细数据
     */
    SysAnnexInfoResponse findById(String id);

    /**
     * 新增
     *
     * @param filter    附件 请求对象
     * @param shareType 附件类型
     */
    Map<String, String> save(MultipartFile filter, ShareType shareType);

    /**
     * 根据主键修改
     *
     * @param updateRequest 附件 请求对象
     */
    String updateById(UpdateSysAnnexRequest updateRequest);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);
}

