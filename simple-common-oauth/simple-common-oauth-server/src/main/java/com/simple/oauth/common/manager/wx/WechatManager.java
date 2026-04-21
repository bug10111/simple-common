package com.simple.oauth.common.manager.wx;

import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.oauth.common.dto.wxLogin.WeChatLoginRequest;
import com.simple.oauth.common.dto.wxLogin.WechatInfoDTO;

/**
 * Created with IntelliJ IDEA
 * Description: 微信登录实现
 *
 * @author qty
 */
public interface WechatManager {

    /**
     * 微信登录
     *
     * @param clientDetails 客户端信息
     * @param request       参数请求
     * @return
     */
    WechatInfoDTO wxLogin(ClientDetails clientDetails, WeChatLoginRequest request);

}
