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
     * 分页列表(请求为空时抛出业务异常;分页默认值见 PageBase,排序固定为创建时间倒序)
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
     * 按主键更新(主键不存在时抛出业务异常,不静默吞掉影响行数为 0 的更新)
     *
     * @param sysSmsTemplate 短信模板对象
     */
    void updateById(SysSmsTemplate sysSmsTemplate);

    /**
     * 删除(逻辑删除;主键集合不能为空,全部主键都不存在时抛出业务异常)
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);

    /**
     * 根据主键获取数据
     *
     * @param id 主键
     * @return SysSmsTemplate 原始表数据;主键不存在时抛出业务异常(不返回 null,调用方无需判空)
     */
    SysSmsTemplate findById(String id);

    /**
     * 根据短信发送类型获取模板
     * <p>send_type 列预期唯一:存在多条未删除同类型模板时将抛出 TooManyResultsException,
     * 上线前请确认表上存在 send_type 唯一索引(或业务层保证唯一)。</p>
     *
     * @param sendType 短信发送类型
     * @return SysSmsTemplate 原始表数据;不存在时抛出业务异常(不返回 null,调用方无需判空)
     */
    SysSmsTemplate findByType(String sendType);

}
