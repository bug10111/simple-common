package com.simple.oauth.manager.login;

import com.simple.common.auth.server.common.entity.AbsUserDetails;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.oauth.common.dto.sysUserLoginKey.FindOneSysUserLoginKeyRequest;
import com.simple.oauth.common.dto.wxLogin.WeChatLoginRequest;
import com.simple.oauth.common.dto.wxLogin.WechatInfoDTO;
import com.simple.oauth.common.entity.sysUser.SysUser;
import com.simple.oauth.common.entity.sysUserLoginKey.SysUserLoginKey;
import com.simple.oauth.common.manager.wx.WechatManager;
import com.simple.oauth.common.service.sysUser.SysUserService;
import com.simple.oauth.common.view.sysUser.SysUserView;
import com.simple.oauth.common.view.sysUserLoginKey.SysUserLoginKeyView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 账号密码登录实现
 *
 * @author qty
 */
@Component
public class WxLoginManager extends OauthAbsLoginManager {

    @Autowired
    private SysUserView sysUserView;

    @Autowired
    private SysUserLoginKeyView sysUserLoginKeyView;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private WechatManager wechatManager;

    @Override
    public boolean support(Object adapter) {
        return adapter instanceof WeChatLoginRequest;
    }

    @Override
    public AbsUserDetails login(ClientDetails clientDetails, Object adapter) {
        WeChatLoginRequest loginRequest = (WeChatLoginRequest) adapter;

        //微信登录
        WechatInfoDTO wechatInfoDTO = wechatManager.wxLogin(clientDetails, loginRequest);

        //校验key
        SysUserLoginKey one = sysUserLoginKeyView.findOne(new FindOneSysUserLoginKeyRequest().setLoginKey(wechatInfoDTO.getOpenId()),
                                                          clientDetails.getClientId());

        //不存在则创建用户
        SysUser sysUser;
        if (one == null) {
            sysUser = sysUserService.save(wechatInfoDTO, clientDetails);
        }

        //存在则获取
        else {
            sysUser = sysUserView.findById(one.getUserId());
        }

        return collectInformation(sysUser, clientDetails, wechatInfoDTO.getOpenId());
    }
}
