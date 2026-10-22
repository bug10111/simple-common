package com.simple.common.sms.common.view.sysSmsCode;

import com.simple.common.sms.common.dto.sysSmsCode.FindAllSysSmsCodeRequest;
import com.simple.common.sms.common.entity.sysSmsCode.SysSmsCode;

import java.util.List;

/**
 * 短信验证码(sys_sms_code)数据库视图接口
 * <p>
 * 注意：本接口的列表查询不应用分页参数(返回全量匹配行),仅用于服务端内部限流等统计场景,
 * 禁止将 {@link FindAllSysSmsCodeRequest} 直接绑定到对外的 HTTP 查询端点。
 * </p>
 *
 * @author qty
 */
public interface SysSmsCodeView {

    /**
     * 列表查询(不应用分页,返回全量匹配行;调用方必须自行携带 date/phone 等条件限制范围)
     *
     * @param findAllRequest 列表参数
     * @return 匹配的验证码记录
     */
    List<SysSmsCode> list(FindAllSysSmsCodeRequest findAllRequest);

    /**
     * 计数查询(仅统计匹配行数,不物化实体,限流等高频场景优先使用本方法)
     *
     * @param findAllRequest 列表参数
     * @return 匹配的记录数
     */
    long count(FindAllSysSmsCodeRequest findAllRequest);

    /**
     * 查询指定手机号/使用状态/发送类型下最新一条验证码记录
     * <p>固定返回至多一条(按创建时间倒序取首条),phone 与 status 为强校验必填条件。</p>
     *
     * @param findAllRequest 查询参数(phone、status 必填,sendType 可选)
     * @return 至多一条的最新记录;无匹配时返回空列表
     */
    List<SysSmsCode> findLatestByPhoneAndStatus(FindAllSysSmsCodeRequest findAllRequest);

    /**
     * 新增
     *
     * @param sysSmsCode 短信验证码对象
     */
    void save(SysSmsCode sysSmsCode);

    /**
     * 按主键更新
     *
     * @param sysSmsCode 短信验证码对象
     */
    void updateById(SysSmsCode sysSmsCode);

    /**
     * 原子消费验证码：仅当记录仍处于「未使用」状态时置为「已使用」并返回是否成功。
     * 用于校验通过后的消费,保证并发重复校验同一验证码时只有一次能成功。
     *
     * @param id 验证码记录主键
     * @return 消费成功返回 true;记录不存在或已被消费返回 false
     */
    boolean consumeByVerify(String id);

    /**
     * 删除(逻辑删除)
     *
     * @param ids 主键集合,不能为空
     */
    void deleteByIds(List<String> ids);

    /**
     * 根据主键获取数据
     *
     * @param id 主键
     * @return SysSmsCode 原始表数据;主键不存在时抛出业务异常(不返回 null,调用方无需判空)
     */
    SysSmsCode findById(String id);

}
