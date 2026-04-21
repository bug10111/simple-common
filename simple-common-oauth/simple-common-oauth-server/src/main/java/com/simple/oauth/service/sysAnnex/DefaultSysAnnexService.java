package com.simple.oauth.service.sysAnnex;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.annex.common.dto.UploadResponse;
import com.simple.common.annex.common.enums.ShareType;
import com.simple.common.annex.common.function.UploadFunction;
import com.simple.common.annex.common.service.AnnexService;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.BeanUtils;
import com.simple.common.eventbus.common.service.EventBusService;
import com.simple.oauth.common.dto.sysAnnex.*;
import com.simple.oauth.common.entity.sysAnnex.SysAnnex;
import com.simple.oauth.common.service.sysAnnex.SysAnnexService;
import com.simple.oauth.common.view.sysAnnex.SysAnnexView;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 附件(sys_annex)默认接口实现
 *
 * @author qty
 */
@Service
@Transactional
class DefaultSysAnnexService implements SysAnnexService {

    @Autowired
    private SysAnnexView sysAnnexView;

    @Autowired
    private AnnexService annexService;

    @Autowired
    private EventBusService eventBusService;

    @Override
    public IPage<SysAnnexPageResponse> findAll(FindAllSysAnnexRequest findAllRequest) {
        var pageInfo = sysAnnexView.findAll(findAllRequest);
        return pageInfo.convert(entity -> BeanUtils.copyProperties(entity, SysAnnexPageResponse.class));
    }

    @Override
    public String get(String id) {
        var sysAnnex = sysAnnexView.findById(id);
        AssertUtils.notEmptyParams(sysAnnex, "主键为[{}]的数据为空", id);

        if (sysAnnex.getShareType().equals(ShareType.PRIVATE)) {
            String userId = LoginUserUtils.getUserTemporary().getUserId();
            AssertUtils.notEmpty(userId, "请获取权限");
            return annexService.generateUrl(sysAnnex.getSaveUrl());
        }
        return sysAnnex.getSaveUrl();
    }

    @Override
    public List<AnnexListResponse> get(List<String> ids) {
        List<SysAnnex> all = sysAnnexView.findAll(ids);
        all.forEach(sysAnnex -> {
            if (sysAnnex.getShareType().equals(ShareType.PRIVATE)) {
                String urlTemporary = annexService.generateUrl(sysAnnex.getSaveUrl());
                sysAnnex.setSaveUrl(urlTemporary);
            }
        });
        return all.stream().map(sysAnnex -> BeanUtils.copyProperties(sysAnnex, AnnexListResponse.class)).toList();
    }

    @Override
    public SysAnnexInfoResponse findById(String id) {
        var sysAnnex = sysAnnexView.findById(id);
        AssertUtils.notEmptyParams(sysAnnex, "主键为[{}]的数据为空", id);
        SysAnnexInfoResponse sysAnnexInfoResponse = BeanUtils.copyProperties(sysAnnex, SysAnnexInfoResponse.class);
        if (sysAnnexInfoResponse.getShareType().equals(ShareType.PRIVATE)) {
            String urlTemporary = annexService.generateUrl(sysAnnexInfoResponse.getSaveUrl());
            sysAnnexInfoResponse.setSaveUrl(urlTemporary);
        }
        return sysAnnexInfoResponse;
    }

    @Override
    @SneakyThrows
    public Map<String, String> save(MultipartFile filter, ShareType shareType) {
        String clientId = LoginUserUtils.getUserTemporary().getClientId();

        @Cleanup var fileInputStream = filter.getInputStream();
        String filename = filter.getOriginalFilename();

        UploadFunction uploadFunction = uploadResponse -> {
            String algorithmValue = uploadResponse.getAlgorithmValue();
            SysAnnex one = sysAnnexView.findOne(new FindOneSysAnnexRequest().setAlgorithmValue(algorithmValue));
            if (one != null) {
                uploadResponse.setSaveUrl(one.getSaveUrl());
                uploadResponse.setIsTrue(true);
                uploadResponse.setExtension(one.getId());
                return true;
            } else {
                return false;
            }
        };
        UploadResponse upload = annexService.upload(filename, clientId, null, shareType, fileInputStream, uploadFunction);
        SysAnnex sysAnnex = BeanUtils.copyProperties(upload, SysAnnex.class);
        if (!upload.getIsTrue()) {
            sysAnnexView.saveOrUpdate(sysAnnex);
        } else {
            sysAnnex.setId(upload.getExtension());
        }

        Map<String, String> map = new HashMap<>();
        map.put("id", sysAnnex.getId());
        map.put("url", shareType == ShareType.PRIVATE ? get(sysAnnex.getId()) : sysAnnex.getSaveUrl());
        map.put("shareType", sysAnnex.getShareType().name());
        return map;
    }

    @Override
    public String updateById(UpdateSysAnnexRequest updateRequest) {
        var entity = BeanUtils.copyProperties(updateRequest, SysAnnex.class);
        sysAnnexView.saveOrUpdate(entity);
        return entity.getId();
    }

    @Override
    public void deleteByIds(List<String> ids) {
        ids.forEach(s -> {
            SysAnnex byId = sysAnnexView.findById(s);
            annexService.delete(byId.getSaveUrl());
        });
        sysAnnexView.deleteByIds(ids);
    }
}

