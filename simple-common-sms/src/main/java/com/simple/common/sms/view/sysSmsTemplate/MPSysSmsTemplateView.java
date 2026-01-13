package com.simple.common.sms.view.sysSmsTemplate;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.sms.common.dto.sysSmsTemplate.FindAllSysSmsTemplateRequest;
import com.simple.common.sms.common.entity.sysSmsTemplate.SysSmsTemplate;
import com.simple.common.sms.common.view.sysSmsTemplate.SysSmsTemplateView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 短信模板(sys_sms_template)数据库视图实现
 *
 * @author qty
 */
@Component
class MPSysSmsTemplateView implements SysSmsTemplateView {

    @Autowired
    private SysSmsTemplateRepository sysSmsTemplateRepository;

    @Override
    public IPage<SysSmsTemplate> findAll(FindAllSysSmsTemplateRequest findAllRequest) {
        LambdaQueryWrapper<SysSmsTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(findAllRequest.getSignName()), SysSmsTemplate::getSignName, findAllRequest.getSignName())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getTemplateCode()), SysSmsTemplate::getTemplateCode, findAllRequest.getTemplateCode())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getDeleted()), SysSmsTemplate::getDeleted, findAllRequest.getDeleted())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getCreateTime()), SysSmsTemplate::getCreateTime, findAllRequest.getCreateTime())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getUpdateTime()), SysSmsTemplate::getUpdateTime, findAllRequest.getUpdateTime());
        return sysSmsTemplateRepository.selectPage(findAllRequest.getPage(SysSmsTemplate.class), queryWrapper);
    }

    @Override
    public void save(SysSmsTemplate sysSmsTemplate) {
        sysSmsTemplateRepository.insert(sysSmsTemplate);
    }

    @Override
    public void updateById(SysSmsTemplate sysSmsTemplate) {
        sysSmsTemplateRepository.updateById(sysSmsTemplate);
    }

    @Override
    public void deleteByIds(List<String> ids) {
        sysSmsTemplateRepository.deleteByIds(ids);
    }

    @Override
    public SysSmsTemplate findById(String id) {
        SysSmsTemplate sysSmsTemplate = sysSmsTemplateRepository.selectById(id);
        AssertUtils.notEmptyParams(sysSmsTemplate, "主键为{}的短信模板不存在", id);
        return sysSmsTemplate;
    }

    @Override
    public SysSmsTemplate findByType(String sendType) {
        SysSmsTemplate sysSmsTemplate = sysSmsTemplateRepository.selectOne(new LambdaQueryWrapper<SysSmsTemplate>().eq(SysSmsTemplate::getSendType, sendType));
        AssertUtils.notEmptyParams(sysSmsTemplate, "类型为{}的短信模板不存在", sendType);
        return sysSmsTemplate;
    }

}

