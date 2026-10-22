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
     * <p>入参契约：entities 不能为 null 或空集合,否则 SQL 退化为无 values 的非法语句抛出语法异常;
     * 主键冲突(id 已存在)时直接抛出重复键异常。</p>
     * <p>数据量契约：单条多值 INSERT 不分批,调用方应自行分批(建议每批不超过 500 条),
     * 避免超出 MySQL max_allowed_packet/占位符上限。</p>
     *
     * @param entities List<SysSmsTemplate> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<SysSmsTemplate> entities);

    /**
     * 批量新增或按主键更新数据（MyBatis原生foreach方法，MP表的自动化操作都无效，需要手动为集合对象赋值）
     * <p>实际语义为 MySQL 的 INSERT ... ON DUPLICATE KEY UPDATE(并非严格的“按主键更新”)：
     * 任一唯一键(主键或 send_type 等唯一索引)冲突时都会转为更新,且只更新业务列
     * (sign_name/template_code/update_time),不改写主键,也不覆盖 create_time 与逻辑删除标记 deleted;
     * 因此不会把逻辑已删除的模板复活,也不会破坏原创建时间。</p>
     * <p>入参契约：entities 不能为 null 或空集合,否则 SQL 语法错误(调用方自行校验)。</p>
     *
     * @param entities List<SysSmsTemplate> 实例对象列表
     * @return 影响行数（MySQL 语义：新增计 1、实际更新计 2、值无变化计 0,不能拿它与 entities.size() 比对判断成功与否）
     * @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
     */
    int insertOrUpdateBatch(@Param("entities") List<SysSmsTemplate> entities);

}
