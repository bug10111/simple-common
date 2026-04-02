package com.simple.oauth.view.sysClientDetails;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.core.utils.AssertUtils;
import com.simple.oauth.common.dto.sysClientDetails.FindAllSysClientDetailsRequest;
import com.simple.oauth.common.entity.sysClientDetails.SysClientDetails;
import com.simple.oauth.common.enums.ServerType;
import com.simple.oauth.common.view.sysClientDetails.SysClientDetailsView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 客户端信息(sys_client_details)数据库视图实现
 *
 * @author 兄台丶请冷静
 */
@Component
class MPSysClientDetailsView implements SysClientDetailsView {

    @Autowired
    private SysClientDetailsRepository sysClientDetailsRepository;

    @Override
    public IPage<SysClientDetails> findAll(FindAllSysClientDetailsRequest findAllRequest) {
        LambdaQueryWrapper<SysClientDetails> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(findAllRequest.getClientName()), SysClientDetails::getClientName, findAllRequest.getClientName())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getStatus()), SysClientDetails::getStatus, findAllRequest.getStatus())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getServer()), SysClientDetails::getServer, findAllRequest.getServer())
                    .orderByDesc(SysClientDetails::getServer, SysClientDetails::getCreateTime);
        return sysClientDetailsRepository.selectPage(findAllRequest.getPage(SysClientDetails.class), queryWrapper);
    }

    @Override
    public List<SysClientDetails> list(String server, ServerType serverType) {
        return sysClientDetailsRepository.selectList(
                        new LambdaQueryWrapper<SysClientDetails>().eq(ObjUtil.isNotEmpty(server), SysClientDetails::getServer, server).eq(SysClientDetails::getServerType, serverType));
    }

    @Override
    public List<SysClientDetails> findAll(ServerType serverType) {
        return sysClientDetailsRepository.selectList(new LambdaQueryWrapper<SysClientDetails>().eq(SysClientDetails::getServerType, serverType));
    }

    @Override
    public SysClientDetails findAllByClientId(String clientId) {
        return sysClientDetailsRepository.selectOne(new LambdaQueryWrapper<SysClientDetails>().eq(SysClientDetails::getClientId, clientId));
    }

    @Override
    public SysClientDetails findById(String id, Boolean allowEmpty) {
        SysClientDetails sysClientDetails = sysClientDetailsRepository.selectById(id);
        if (!allowEmpty && sysClientDetails == null) {
            AssertUtils.errorParams("主键为[{}]的数据为空", id);
        }
        return sysClientDetails;
    }

    @Override
    public SysClientDetails findByNameAndNeqId(String name, String id) {
        return sysClientDetailsRepository.selectOne(
                        new LambdaQueryWrapper<SysClientDetails>().eq(SysClientDetails::getClientName, name).ne(ObjUtil.isNotEmpty(id), SysClientDetails::getId, id));
    }

    @Override
    public SysClientDetails findByClientIdAndNeqId(String clientId, String id) {
        return sysClientDetailsRepository.selectOne(
                        new LambdaQueryWrapper<SysClientDetails>().eq(SysClientDetails::getClientId, clientId).ne(ObjUtil.isNotEmpty(id), SysClientDetails::getId, id));
    }

    @Override
    public SysClientDetails findByClientId(String clientId) {
        return sysClientDetailsRepository.selectOne(new LambdaQueryWrapper<SysClientDetails>().eq(SysClientDetails::getClientId, clientId));
    }

    @Override
    public void save(SysClientDetails sysClientDetails) {
        if (sysClientDetails.getId() == null) {
            sysClientDetailsRepository.insert(sysClientDetails);
        } else {
            sysClientDetailsRepository.updateById(sysClientDetails);
        }
    }

    @Override
    public void saves(List<SysClientDetails> list) {
        sysClientDetailsRepository.insertBatch(list);
    }

    @Override
    public void deleteByIds(List<String> ids) {
        sysClientDetailsRepository.deleteByIds(ids);
    }

}

