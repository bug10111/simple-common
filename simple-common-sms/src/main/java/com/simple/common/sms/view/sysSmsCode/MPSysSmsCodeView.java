package com.simple.common.sms.view.sysSmsCode;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.sms.common.dto.sysSmsCode.FindAllSysSmsCodeRequest;
import com.simple.common.sms.common.entity.sysSmsCode.SysSmsCode;
import com.simple.common.mp.common.enums.Status;
import com.simple.common.sms.common.view.sysSmsCode.SysSmsCodeView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 短信验证码(sys_sms_code)数据库视图实现
 *
 * @author qty
 */
@Component
class MPSysSmsCodeView implements SysSmsCodeView {

    @Autowired
    private SysSmsCodeRepository sysSmsCodeRepository;

    @Override
    public List<SysSmsCode> list(FindAllSysSmsCodeRequest findAllRequest) {
        return sysSmsCodeRepository.selectList(buildQueryWrapper(findAllRequest));
    }

    @Override
    public long count(FindAllSysSmsCodeRequest findAllRequest) {
        // 高频限流场景只取行数,不物化整表实体
        Long count = sysSmsCodeRepository.selectCount(buildQueryWrapper(findAllRequest));
        return count == null ? 0L : count;
    }

    @Override
    public List<SysSmsCode> findLatestByPhoneAndStatus(FindAllSysSmsCodeRequest findAllRequest) {
        // 手机号与使用状态为强约束,缺失时直接失败,避免空条件导致跨手机号误匹配
        AssertUtils.notEmpty(findAllRequest.getPhone(), "查询验证码时手机号不能为空");
        AssertUtils.notEmpty(findAllRequest.getStatus(), "查询验证码时使用状态不能为空");

        LambdaQueryWrapper<SysSmsCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysSmsCode::getPhone, findAllRequest.getPhone())
                    .eq(SysSmsCode::getStatus, findAllRequest.getStatus())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getSendType()), SysSmsCode::getSendType, findAllRequest.getSendType())
                    .orderByDesc(SysSmsCode::getCreateTime)
                    .orderByDesc(SysSmsCode::getId);
        // 手机号+状态命中行数量级很小,取首条即最新记录,避免 last("limit 1") 的方言依赖并保持走拦截器链
        List<SysSmsCode> list = sysSmsCodeRepository.selectList(queryWrapper);
        return list.isEmpty() ? List.of() : list.subList(0, 1);
    }

    @Override
    public void save(SysSmsCode sysSmsCode) {
        sysSmsCodeRepository.insert(sysSmsCode);
    }

    @Override
    public void updateById(SysSmsCode sysSmsCode) {
        sysSmsCodeRepository.updateById(sysSmsCode);
    }

    @Override
    public boolean consumeByVerify(String id) {
        // 原子消费:仅当仍为未使用时置为已使用,并发重复校验同一验证码只有一次成功
        return sysSmsCodeRepository.update(null,
                new LambdaUpdateWrapper<SysSmsCode>()
                        .eq(SysSmsCode::getId, id)
                        .eq(SysSmsCode::getStatus, Status.NOT_USED)
                        .set(SysSmsCode::getStatus, Status.USED)) > 0;
    }

    @Override
    public void deleteByIds(List<String> ids) {
        // MyBatis-Plus deleteBatchIds 对空集合抛 IllegalArgumentException,前置校验给出明确业务提示
        AssertUtils.notEmpty(ids, "待删除的验证码主键不能为空");
        sysSmsCodeRepository.deleteBatchIds(ids);
    }

    @Override
    public SysSmsCode findById(String id) {
        SysSmsCode sysSmsCode = sysSmsCodeRepository.selectById(id);
        AssertUtils.notEmpty(sysSmsCode, "主键为{}的短信验证码不存在", id);
        return sysSmsCode;
    }

    /**
     * 构建列表/计数共用的查询条件(仅按 DTO 中存在的过滤字段;deleted 为 @TableLogic 列由框架自动追加 deleted=0)
     */
    private LambdaQueryWrapper<SysSmsCode> buildQueryWrapper(FindAllSysSmsCodeRequest findAllRequest) {
        LambdaQueryWrapper<SysSmsCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(findAllRequest.getDate()), SysSmsCode::getDate, findAllRequest.getDate())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getPhone()), SysSmsCode::getPhone, findAllRequest.getPhone())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getCode()), SysSmsCode::getCode, findAllRequest.getCode())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getIp()), SysSmsCode::getIp, findAllRequest.getIp())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getReqStatus()), SysSmsCode::getReqStatus, findAllRequest.getReqStatus())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getReqResults()), SysSmsCode::getReqResults, findAllRequest.getReqResults())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getStatus()), SysSmsCode::getStatus, findAllRequest.getStatus());
        return queryWrapper;
    }

}
