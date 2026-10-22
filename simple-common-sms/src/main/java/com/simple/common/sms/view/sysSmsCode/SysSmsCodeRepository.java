package com.simple.common.sms.view.sysSmsCode;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simple.common.sms.common.entity.sysSmsCode.SysSmsCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 短信验证码(sys_sms_code)数据库访问层
 *
 * @author qty
 */
@Mapper
public interface SysSmsCodeRepository extends BaseMapper<SysSmsCode> {

    /**
     * 批量新增数据（MyBatis原生foreach方法，MP表的自动化操作都无效，需要手动为集合对象赋值）
     * <p>入参契约：entities 不能为 null 或空集合,否则 SQL 退化为无 values 的非法语句抛出语法异常;
     * 主键冲突(id 已存在)时直接抛出重复键异常;实体主键由调用方保证非空。</p>
     *
     * @param entities List<SysSmsCode> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<SysSmsCode> entities);

    /**
     * 批量新增或按主键更新数据（MyBatis原生foreach方法，MP表的自动化操作都无效，需要手动为集合对象赋值）
     * <p>语义为 MySQL 的 INSERT ... ON DUPLICATE KEY UPDATE：任一唯一键(主键或其他唯一索引)冲突即转为更新,
     * 且只更新业务列(date/send_type/phone/code/ip/req_status/req_results/status/update_time),
     * 不会改写主键,也不会覆盖 create_time 与逻辑删除标记 deleted;MP 自动填充对本语句无效,
     * 调用方需自行保证实体中业务字段完整(尤其 update_time)。</p>
     * <p>入参契约：entities 不能为 null 或空集合,否则 SQL 语法错误(调用方自行校验)。</p>
     *
     * @param entities List<SysSmsCode> 实例对象列表
     * @return 影响行数（MySQL 语义：新增计 1、实际更新计 2、值无变化计 0,不能拿它与 entities.size() 比对判断成功与否）
     * @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
     */
    int insertOrUpdateBatch(@Param("entities") List<SysSmsCode> entities);

}
