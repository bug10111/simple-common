package com.simple.oauth.view.sysAnnex;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.oauth.common.dto.sysAnnex.FindAllSysAnnexRequest;
import com.simple.oauth.common.dto.sysAnnex.FindOneSysAnnexRequest;
import com.simple.oauth.common.entity.sysAnnex.SysAnnex;
import com.simple.oauth.common.view.sysAnnex.SysAnnexView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 附件(sys_annex)数据库视图实现
 *
 * @author qty
 */
@Component
class MPSysAnnexView implements SysAnnexView {

    @Autowired
    private SysAnnexRepository repository;

    @Override
    public IPage<SysAnnex> findAll(FindAllSysAnnexRequest findAllRequest) {
        LambdaQueryWrapper<SysAnnex> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(ObjUtil.isNotEmpty(findAllRequest.getName()), SysAnnex::getName, findAllRequest.getName())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getAlgorithmValue()), SysAnnex::getAlgorithmValue, findAllRequest.getAlgorithmValue())
                    .likeRight(ObjUtil.isNotEmpty(findAllRequest.getSaveUrl()), SysAnnex::getSaveUrl, findAllRequest.getSaveUrl())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getShareType()), SysAnnex::getShareType, findAllRequest.getShareType())
                    .likeRight(ObjUtil.isNotEmpty(findAllRequest.getApplicationName()), SysAnnex::getApplicationName, findAllRequest.getApplicationName());
        return repository.selectPage(findAllRequest.getPage(SysAnnex.class), queryWrapper);
    }

    @Override
    public List<SysAnnex> findAll(List<String> ids) {
        return repository.selectList(new LambdaQueryWrapper<SysAnnex>().in(SysAnnex::getId, ids).select(SysAnnex::getId, SysAnnex::getSaveUrl, SysAnnex::getName, SysAnnex::getShareType));
    }

    @Override
    public SysAnnex findOne(FindOneSysAnnexRequest findOneRequest, FindOneSysAnnexRequest neRequest) {
        LambdaQueryWrapper<SysAnnex> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(findOneRequest.getId()), SysAnnex::getId, findOneRequest.getId())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getName()), SysAnnex::getName, findOneRequest.getName())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getTotalSize()), SysAnnex::getTotalSize, findOneRequest.getTotalSize())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getAlgorithmValue()), SysAnnex::getAlgorithmValue, findOneRequest.getAlgorithmValue())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getAlgorithmType()), SysAnnex::getAlgorithmType, findOneRequest.getAlgorithmType())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getSuffix()), SysAnnex::getSuffix, findOneRequest.getSuffix())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getSaveUrl()), SysAnnex::getSaveUrl, findOneRequest.getSaveUrl())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getShareType()), SysAnnex::getShareType, findOneRequest.getShareType())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getApplicationName()), SysAnnex::getApplicationName, findOneRequest.getApplicationName())
                    .ne(ObjUtil.isNotEmpty(neRequest.getId()), SysAnnex::getId, neRequest.getId())
                    .ne(ObjUtil.isNotEmpty(neRequest.getName()), SysAnnex::getName, neRequest.getName())
                    .ne(ObjUtil.isNotEmpty(neRequest.getTotalSize()), SysAnnex::getTotalSize, neRequest.getTotalSize())
                    .ne(ObjUtil.isNotEmpty(neRequest.getAlgorithmValue()), SysAnnex::getAlgorithmValue, neRequest.getAlgorithmValue())
                    .ne(ObjUtil.isNotEmpty(neRequest.getAlgorithmType()), SysAnnex::getAlgorithmType, neRequest.getAlgorithmType())
                    .ne(ObjUtil.isNotEmpty(neRequest.getSuffix()), SysAnnex::getSuffix, neRequest.getSuffix())
                    .ne(ObjUtil.isNotEmpty(neRequest.getSaveUrl()), SysAnnex::getSaveUrl, neRequest.getSaveUrl())
                    .ne(ObjUtil.isNotEmpty(neRequest.getShareType()), SysAnnex::getShareType, neRequest.getShareType())
                    .ne(ObjUtil.isNotEmpty(neRequest.getApplicationName()), SysAnnex::getApplicationName, neRequest.getApplicationName());

        List<SysAnnex> list = repository.selectList(queryWrapper);
        if (list.isEmpty()) {
            return null;
        } else if (list.size() > 1) {
            AssertUtils.error("数据异常", "参数为[{}]查询只需要一条数据，但是查询出来多条", JsonUtils.toJsonStr(findOneRequest));
        }
        return list.get(0);
    }

    @Override
    public SysAnnex findById(String id) {
        return repository.selectById(id);
    }

    @Override
    public void saveOrUpdate(SysAnnex sysAnnex) {
        if (sysAnnex.getId() == null) {
            repository.insert(sysAnnex);
        } else {
            repository.updateById(sysAnnex);
        }
    }

    @Override
    public void saves(List<SysAnnex> list) {
        repository.insertBatch(list);
    }

    @Override
    public void deleteByIds(List<String> ids) {
        repository.deleteByIds(ids);
    }
}

