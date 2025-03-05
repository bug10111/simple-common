package com.simple.common.sms.view.sysSmsCode;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.sms.common.dto.sysSmsCode.FindAllSysSmsCodeRequest;
import com.simple.common.sms.common.entity.sysSmsCode.SysSmsCode;
import com.simple.common.sms.common.view.sysSmsCode.SysSmsCodeView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 短信验证码(sys_code_record)数据库视图实现
 *
 * @author 兄台丶请冷静
 */
@Component
class MPSysSmsCodeView implements SysSmsCodeView {

    @Autowired
    private SysSmsCodeRepository sysSmsCodeRepository;

    @Override
    public List<SysSmsCode> list(FindAllSysSmsCodeRequest findAllRequest) {
        LambdaQueryWrapper<SysSmsCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(findAllRequest.getDate()), SysSmsCode::getDate, findAllRequest.getDate())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getPhone()), SysSmsCode::getPhone, findAllRequest.getPhone())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getCode()), SysSmsCode::getCode, findAllRequest.getCode())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getIp()), SysSmsCode::getIp, findAllRequest.getIp())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getReqResults()), SysSmsCode::getReqResults, findAllRequest.getReqResults())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getStatus()), SysSmsCode::getStatus, findAllRequest.getStatus())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getDeleted()), SysSmsCode::getDeleted, findAllRequest.getDeleted())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getCreateTime()), SysSmsCode::getCreateTime, findAllRequest.getCreateTime())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getUpdateTime()), SysSmsCode::getUpdateTime, findAllRequest.getUpdateTime());
        return sysSmsCodeRepository.selectList(queryWrapper);
    }

    @Override
    public List<SysSmsCode> findByTimeAndPhoneAndState(FindAllSysSmsCodeRequest findAllRequest) {
        LambdaQueryWrapper<SysSmsCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(findAllRequest.getPhone()), SysSmsCode::getPhone, findAllRequest.getPhone())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getStatus()), SysSmsCode::getStatus, findAllRequest.getStatus())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getSendType()), SysSmsCode::getSendType, findAllRequest.getSendType())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getCode()), SysSmsCode::getCode, findAllRequest.getCode())
                    .orderByDesc(ObjUtil.isNotEmpty(findAllRequest.getCreateTime()), SysSmsCode::getCreateTime)
                    .last("limit 1");
        return sysSmsCodeRepository.selectList(queryWrapper);
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
    public void deleteByIds(List<String> ids) {
        sysSmsCodeRepository.deleteBatchIds(ids);
    }

    @Override
    public SysSmsCode findById(String id) {
        SysSmsCode sysSmsCode = sysSmsCodeRepository.selectById(id);
        AssertUtils.notEmptyParams(sysSmsCode, "主键为{}的短信验证码不存在", id);
        return sysSmsCode;
    }

}

