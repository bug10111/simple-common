package com.simple.common.sms.common.view.sysSmsCode;

import com.simple.common.sms.common.dto.sysSmsCode.FindAllSysSmsCodeRequest;
import com.simple.common.sms.common.entity.sysSmsCode.SysSmsCode;

import java.util.List;

/**
 * 短信验证码(sys_code_record)数据库视图接口
 *
 * @author qty
 */
public interface SysSmsCodeView {

    /**
     * 列表
     *
     * @param findAllRequest 列表参数
     */
    List<SysSmsCode> list(FindAllSysSmsCodeRequest findAllRequest);

    /**
     * 列表
     *
     * @param findAllRequest 列表参数
     */
    List<SysSmsCode> findByTimeAndPhoneAndState(FindAllSysSmsCodeRequest findAllRequest);

    /**
     * 新增
     *
     * @param sysSmsCode 短信验证码对象
     */
    void save(SysSmsCode sysSmsCode);

    /**
     * 修改
     *
     * @param sysSmsCode 短信验证码对象
     */
    void updateById(SysSmsCode sysSmsCode);

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
     * @return SysCodeRecord 原始表数据
     */
    SysSmsCode findById(String id);

}

