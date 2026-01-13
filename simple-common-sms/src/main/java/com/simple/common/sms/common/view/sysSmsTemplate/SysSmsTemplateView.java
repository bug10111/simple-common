package com.simple.common.sms.common.view.sysSmsTemplate;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.sms.common.dto.sysSmsTemplate.FindAllSysSmsTemplateRequest;
import com.simple.common.sms.common.entity.sysSmsTemplate.SysSmsTemplate;

import java.util.List;

/**
 * 短信模板(sys_sms_template)数据库视图接口
 *
 * @author qty
 */
public interface SysSmsTemplateView {

    /**
     * 分页列表
     *
     * @param findAllRequest 分页参数
     * @return 分页数据
     */
    IPage<SysSmsTemplate> findAll(FindAllSysSmsTemplateRequest findAllRequest);

    /**
     * 新增
     *
     * @param sysSmsTemplate 短信模板对象
     */
    void save(SysSmsTemplate sysSmsTemplate);

    /**
     * 修改
     *
     * @param sysSmsTemplate 短信模板对象
     */
    void updateById(SysSmsTemplate sysSmsTemplate);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);

    /**
     * 根据主键获取数据
     *
     * @param id 主键
     * @return SysSmsTemplate 原始表数据
     */
    SysSmsTemplate findById(String id);

    /**
     * 根据主键获取数据
     *
     * @param sendType type
     * @return SysSmsTemplate 原始表数据
     */
    SysSmsTemplate findByType(String sendType);

}

