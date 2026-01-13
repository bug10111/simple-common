package com.simple.common.sms.view.sysSmsTemplate;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simple.common.sms.common.entity.sysSmsTemplate.SysSmsTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 短信模板(sys_sms_template)数据库访问层
 *
 * @author qty
 */
@Mapper
public interface SysSmsTemplateRepository extends BaseMapper<SysSmsTemplate> {

    /**
     * 批量新增数据（MyBatis原生foreach方法，MP表的自动化操作都无效，需要手动为集合对象赋值）
     *
     * @param entities List<SysSmsTemplate> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<SysSmsTemplate> entities);

    /**
     * 批量新增或按主键更新数据（MyBatis原生foreach方法，MP表的自动化操作都无效，需要手动为集合对象赋值）
     *
     * @param entities List<SysSmsTemplate> 实例对象列表
     * @return 影响行数
     * @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
     */
    int insertOrUpdateBatch(@Param("entities") List<SysSmsTemplate> entities);

}

