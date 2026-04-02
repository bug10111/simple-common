package com.simple.oauth.service.sysClientDetails;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.auth.server.common.event.SecretEvent;
import com.simple.common.auth.server.common.manager.secret.SecretManager;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.Base64Utils;
import com.simple.common.core.utils.CryptoUtil;
import com.simple.common.eventbus.common.service.EventBusService;
import com.simple.oauth.common.dto.api.ApiSysClientDetailsResponse;
import com.simple.oauth.common.dto.sysClientDetails.*;
import com.simple.oauth.common.entity.sysClientDetails.SysClientDetails;
import com.simple.oauth.common.event.sysClientDetails.SysClientDetailsCreatedEvent;
import com.simple.oauth.common.service.sysClientDetails.SysClientDetailsService;
import com.simple.oauth.common.view.sysClientDetails.SysClientDetailsView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Service
public class DefaultSysClientDetailsService implements SysClientDetailsService {

    @Autowired
    private SysClientDetailsView sysClientDetailsView;

    @Autowired
    private EventBusService eventBusService;

    @Autowired(required = false)
    private SecretManager secretManager;

    @Override
    public IPage<SysClientDetailsPageResponse> findAll(FindAllSysClientDetailsRequest request) {
        IPage<SysClientDetails> page = sysClientDetailsView.findAll(request);
        return page.convert(sysClientDetails -> BeanUtil.toBean(sysClientDetails, SysClientDetailsPageResponse.class));
    }

    @Override
    public List<ApiSysClientDetailsResponse> list(String appName) {
        List<SysClientDetails> list = sysClientDetailsView.list(appName);
        return list.stream()
                .map(sysClientDetails -> BeanUtil.toBean(sysClientDetails, ApiSysClientDetailsResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public SysClientDetailsInfoResponse findById(String id) {
        SysClientDetails sysClientDetails = sysClientDetailsView.getById(id);
        AssertUtils.notNull(sysClientDetails, "客户端信息不存在");
        return BeanUtil.toBean(sysClientDetails, SysClientDetailsInfoResponse.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String save(CreateSysClientDetailsRequest request) {
        SysClientDetails sysClientDetails = BeanUtil.toBean(request, SysClientDetails.class);
        
        // 生成默认密钥
        if (ObjUtil.isEmpty(sysClientDetails.getHsKey())) {
            byte[] aesKey = CryptoUtil.generateSymmetricKey(CryptoUtil.SymmetricAlgorithmType.AES_GCM);
            sysClientDetails.setHsKey(Base64Utils.encode(aesKey));
        }
        
        // 生成 RSA 密钥对
        if (ObjUtil.isEmpty(sysClientDetails.getRsaPublic()) || ObjUtil.isEmpty(sysClientDetails.getRsaPrivate())) {
            java.security.KeyPair keyPair = CryptoUtil.generateKeyPair(CryptoUtil.AsymmetricAlgorithmType.RSA_OAEP);
            sysClientDetails.setRsaPublic(Base64Utils.encode(keyPair.getPublic().getEncoded()));
            sysClientDetails.setRsaPrivate(Base64Utils.encode(keyPair.getPrivate().getEncoded()));
        }
        
        sysClientDetailsView.saveAndLoading(sysClientDetails);
        
        // 发布客户端创建事件
        SysClientDetailsCreatedEvent event = new SysClientDetailsCreatedEvent();
        event.setClientId(sysClientDetails.getClientId());
        event.setClientName(sysClientDetails.getClientName());
        eventBusService.push(event);
        
        log.info("客户端创建成功: clientId={}", sysClientDetails.getClientId());
        return sysClientDetails.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(UpdateSysClientDetailsRequest request) {
        SysClientDetails sysClientDetails = BeanUtil.toBean(request, SysClientDetails.class);
        
        // 获取旧的客户端信息
        SysClientDetails oldClientDetails = sysClientDetailsView.getById(request.getId());
        AssertUtils.notNull(oldClientDetails, "客户端信息不存在");
        
        sysClientDetailsView.updateById(sysClientDetails);
        
        log.info("客户端更新成功: clientId={}", oldClientDetails.getClientId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPwd(RestSysClientRequest request) {
        SysClientDetails sysClientDetails = sysClientDetailsView.getById(request.getId());
        AssertUtils.notNull(sysClientDetails, "客户端信息不存在");
        
        sysClientDetails.setClientSecret(CryptoUtil.hashPassword(request.getPassword()));
        sysClientDetailsView.updateById(sysClientDetails);
        
        log.info("客户端密码重置成功: clientId={}", sysClientDetails.getClientId());
    }

    @Override
    public Map<String, Object> createKey() {
        // 生成 AES 密钥
        byte[] aesKey = CryptoUtil.generateSymmetricKey(CryptoUtil.SymmetricAlgorithmType.AES_GCM);
        String aesKeyStr = Base64Utils.encode(aesKey);
        
        // 生成 RSA 密钥对
        java.security.KeyPair keyPair = CryptoUtil.generateKeyPair(CryptoUtil.AsymmetricAlgorithmType.RSA_OAEP);
        String publicKeyStr = Base64Utils.encode(keyPair.getPublic().getEncoded());
        String privateKeyStr = Base64Utils.encode(keyPair.getPrivate().getEncoded());
        
        return Map.of(
                "hsKey", aesKeyStr,
                "rsaPublic", publicKeyStr,
                "rsaPrivate", privateKeyStr
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSecret(String id, String hsKey, String rsaPublic, String rsaPrivate) {
        SysClientDetails sysClientDetails = sysClientDetailsView.getById(id);
        AssertUtils.notNull(sysClientDetails, "客户端信息不存在");
        
        if (hsKey != null && !hsKey.isEmpty()) {
            sysClientDetails.setHsKey(hsKey);
        }
        if (rsaPublic != null && !rsaPublic.isEmpty()) {
            sysClientDetails.setRsaPublic(rsaPublic);
        }
        if (rsaPrivate != null && !rsaPrivate.isEmpty()) {
            sysClientDetails.setRsaPrivate(rsaPrivate);
        }
        
        sysClientDetailsView.updateById(sysClientDetails);
        
        // 发布秘钥更新事件
        SecretEvent event = new SecretEvent();
        event.setClientId(sysClientDetails.getClientId());
        event.setOperation(SecretEvent.Operation.UPDATE);
        eventBusService.push(event);
        
        log.info("客户端秘钥更新成功: clientId={}", sysClientDetails.getClientId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> regenerateSecret(String id, String[] secretTypes) {
        SysClientDetails sysClientDetails = sysClientDetailsView.getById(id);
        AssertUtils.notNull(sysClientDetails, "客户端信息不存在");
        
        Map<String, Object> result = new java.util.HashMap<>();
        
        for (String type : secretTypes) {
            switch (type) {
                case "hsKey":
                    byte[] newAesKey = CryptoUtil.generateSymmetricKey(CryptoUtil.SymmetricAlgorithmType.AES_GCM);
                    String newAesKeyStr = Base64Utils.encode(newAesKey);
                    sysClientDetails.setHsKey(newAesKeyStr);
                    result.put("hsKey", newAesKeyStr);
                    break;
                case "rsaPublic":
                case "rsaPrivate":
                    java.security.KeyPair newKeyPair = CryptoUtil.generateKeyPair(CryptoUtil.AsymmetricAlgorithmType.RSA_OAEP);
                    String newPublicKeyStr = Base64Utils.encode(newKeyPair.getPublic().getEncoded());
                    String newPrivateKeyStr = Base64Utils.encode(newKeyPair.getPrivate().getEncoded());
                    sysClientDetails.setRsaPublic(newPublicKeyStr);
                    sysClientDetails.setRsaPrivate(newPrivateKeyStr);
                    result.put("rsaPublic", newPublicKeyStr);
                    result.put("rsaPrivate", newPrivateKeyStr);
                    break;
            }
        }
        
        sysClientDetailsView.updateById(sysClientDetails);
        
        // 发布秘钥更新事件
        SecretEvent event = new SecretEvent();
        event.setClientId(sysClientDetails.getClientId());
        event.setOperation(SecretEvent.Operation.UPDATE);
        eventBusService.push(event);
        
        log.info("客户端秘钥重新生成成功: clientId={}, types={}", sysClientDetails.getClientId(), secretTypes);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(String id) {
        SysClientDetails sysClientDetails = sysClientDetailsView.getById(id);
        AssertUtils.notNull(sysClientDetails, "客户端信息不存在");
        
        sysClientDetailsView.deleteById(id);
        
        log.info("客户端删除成功: clientId={}", sysClientDetails.getClientId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<String> ids) {
        for (String id : ids) {
            deleteById(id);
        }
    }
}