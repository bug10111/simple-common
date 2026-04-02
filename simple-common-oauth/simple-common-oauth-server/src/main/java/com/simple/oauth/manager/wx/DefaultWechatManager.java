package com.simple.oauth.manager.wx;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.AES;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.exception.DefaultExceptionEnum;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.HttpUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.oauth.common.dto.wxLogin.WeChatLoginRequest;
import com.simple.oauth.common.dto.wxLogin.WeChatLoginResponse;
import com.simple.oauth.common.dto.wxLogin.WechatInfoDTO;
import com.simple.oauth.common.entity.sysClientDetails.SysClientDetails;
import com.simple.oauth.common.manager.wx.WechatManager;
import com.simple.oauth.common.properties.OauthProperties;
import com.simple.oauth.common.view.sysClientDetails.SysClientDetailsView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Component
public class DefaultWechatManager implements WechatManager {

    //地址
    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=%s";

    //授权（必填）
    private static final String GRANT_TYPE = "authorization_code";

    @Autowired
    private OauthProperties oauthProperties;

    @Autowired
    private SysClientDetailsView sysClientDetailsView;

    @Override
    public WechatInfoDTO wxLogin(ClientDetails clientDetails, WeChatLoginRequest request) {
        SysClientDetails byClientId = sysClientDetailsView.findByClientId(clientDetails.getClientId());

        String requestUrl = String.format(CODE2SESSION_URL, byClientId.getWxAppId(), byClientId.getWxAppSecret(), request.getCode(), GRANT_TYPE);
        Optional<WeChatLoginResponse> weChatLoginResponse = HttpUtils.get(requestUrl, null, oauthProperties.getWxLoginTimeout(), WeChatLoginResponse.class);
        weChatLoginResponse.orElseThrow(() -> new DefaultException(DefaultExceptionEnum.ERROR, "微信响应异常"));

        WeChatLoginResponse response = weChatLoginResponse.get();

        AssertUtils.notEmpty(response.getOpenid(), "未获取到openId");
        //        WechatInfoDTO decode = decode(request.getEncryptedData(), request.getIv(), response.getSessionKey());
        WechatInfoDTO decode = new WechatInfoDTO();
        return decode.setOpenId(response.getOpenid()).setUnionId(response.getUnionId());
    }

    /**
     * 将微信encryptedData解密为json字符串
     */
    protected WechatInfoDTO decode(String encryptedData, String iv, String sessionKey) {
        try {
            AES aes = new AES(Mode.CBC, Padding.PKCS5Padding, Base64.getDecoder().decode(sessionKey), Base64.getDecoder().decode(iv));
            var json = new String(aes.decrypt(encryptedData), StandardCharsets.UTF_8);
            return JsonUtils.toJsonObj(json, WechatInfoDTO.class);
        } catch (Exception e) {
            AssertUtils.error(e.getMessage(), "解密微信数据出错：Data：{},IV：{},SessionKey：{}==>", encryptedData, iv, sessionKey, e);
        }
        return null;
    }
}
