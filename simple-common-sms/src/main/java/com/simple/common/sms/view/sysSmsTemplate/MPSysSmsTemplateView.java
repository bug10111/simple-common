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
        // 入参前置校验:空请求会退化成全表扫描,直接拒绝并给出明确提示
        AssertUtils.notEmpty(findAllRequest, "查询请求不能为空");
        LambdaQueryWrapper<SysSmsTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(findAllRequest.getSignName()), SysSmsTemplate::getSignName, findAllRequest.getSignName())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getTemplateCode()), SysSmsTemplate::getTemplateCode, findAllRequest.getTemplateCode())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getSendType()), SysSmsTemplate::getSendType, findAllRequest.getSendType())
                    // 固定排序保证分页结果稳定,避免翻页时数据重复或漏页
                    .orderByDesc(SysSmsTemplate::getCreateTime)
                    .orderByDesc(SysSmsTemplate::getId);
        return sysSmsTemplateRepository.selectPage(findAllRequest.getPage(SysSmsTemplate.class), queryWrapper);
    }

    @Override
    public void save(SysSmsTemplate sysSmsTemplate) {
        sysSmsTemplateRepository.insert(sysSmsTemplate);
    }

    @Override
    public void updateById(SysSmsTemplate sysSmsTemplate) {
        // 校验影响行数:更新不存在的主键应报错,而不是静默成功
        int rows = sysSmsTemplateRepository.updateById(sysSmsTemplate);
        AssertUtils.isTrue(rows > 0, "主键为{}的短信模板不存在，更新失败", sysSmsTemplate.getId());
    }

    @Override
    public void deleteByIds(List<String> ids) {
        // MyBatis-Plus deleteBatchIds 对空集合抛 IllegalArgumentException,前置校验给出业务提示
        AssertUtils.notEmpty(ids, "待删除的短信模板主键不能为空");
        int rows = sysSmsTemplateRepository.deleteByIds(ids);
        AssertUtils.isTrue(rows > 0, "待删除的短信模板不存在或已删除");
    }

    @Override
    public SysSmsTemplate findById(String id) {
        SysSmsTemplate sysSmsTemplate = sysSmsTemplateRepository.selectById(id);
        AssertUtils.notEmpty(sysSmsTemplate, "主键为{}的短信模板不存在", id);
        return sysSmsTemplate;
    }

    @Override
    public SysSmsTemplate findByType(String sendType) {
        // send_type 预期唯一(见表唯一索引约束);命中多行时 MyBatis-Plus 会抛 TooManyResultsException,属数据异常应暴露
        SysSmsTemplate sysSmsTemplate = sysSmsTemplateRepository.selectOne(
                new LambdaQueryWrapper<SysSmsTemplate>().eq(SysSmsTemplate::getSendType, sendType));
        AssertUtils.notEmpty(sysSmsTemplate, "类型为{}的短信模板不存在", sendType);
        return sysSmsTemplate;
    }

}
