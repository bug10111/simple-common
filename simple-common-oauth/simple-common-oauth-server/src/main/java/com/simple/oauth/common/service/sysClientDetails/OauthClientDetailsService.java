package com.simple.oauth.common.service.sysClientDetails;

import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.service.client.AbsClientDetailsService;
import com.simple.common.core.utils.CryptoUtil;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.BeanUtils;
import com.simple.common.mp.common.enums.Status;
import com.simple.oauth.common.entity.sysClientDetails.SysClientDetails;
import com.simple.oauth.common.view.sysClientDetails.SysClientDetailsView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Service
public class OauthClientDetailsService extends AbsClientDetailsService {

    @Autowired
    private SysClientDetailsView clientDetailsView;

    @Override
    public ClientDetails checkClientDetails(String clientId, String clientSecret) {
        SysClientDetails clientDetails = clientDetailsView.findByClientId(clientId);
        AssertUtils.isTrue(ObjUtil.isNotNull(clientDetails), "客户端不存在");
        AssertUtils.isTrue(CryptoUtil.checkPassword(clientSecret, clientDetails.getClientSecret()), "客户端密钥错误");
        AssertUtils.isTrue(clientDetails.getStatus() == Status.ON, "客户端未启用，请联系管理员");
        return BeanUtils.copyProperties(clientDetails, ClientDetails.class);
    }
}
