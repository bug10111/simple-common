package com.simple.common.auth.server.service.login;

import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.entity.login.UserTemporary;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.manager.token.TokenManager;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.auth.server.common.adapter.LoginTypeAdapter;
import com.simple.common.auth.server.common.entity.AbsUserDetails;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.entity.TokenData;
import com.simple.common.auth.server.common.manager.login.LoginManager;
import com.simple.common.auth.server.common.manager.user.LoginUserOperationManager;
import com.simple.common.auth.server.common.process.LoginSucProcess;
import com.simple.common.auth.server.common.service.client.ClientDetailsService;
import com.simple.common.auth.server.common.service.login.LoginService;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.HttpServletUtils;
import com.simple.common.eventbus.common.service.EventBusService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 默认登录服务实现。
 *
 * @author qty (修复 refresh bug 版本)
 */
@Slf4j
@Service
public class DefaultLoginService implements LoginService {

    @Autowired
    private ClientDetailsService clientDetailsService;

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private LoginUserOperationManager loginUserOperationManager;

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Autowired
    private List<LoginSucProcess> list;


    @Override
    public Map<String, String> login(Object adapter, LoginTypeAdapter loginType) {
        HttpServletRequest request = HttpServletUtils.getRequest();

        // 校验并获取客户端信息
        ClientDetails clientDetails = clientDetailsService.getClientDetails(request);

        // 获取用户信息
        AbsUserDetails absUserDetails = getAbsUserDetails(clientDetails, adapter, loginType);

        // 构建token数据
        TokenData tokenData = new TokenData();
        tokenData.create(clientDetails, absUserDetails);

        // 生成token
        String accessToken = tokenManager.create(tokenData.getAccessTokenMap());
        String refreshToken = tokenManager.create(tokenData.getRefreshTokenMap());

        // 添加返回数据
        Map<String, String> loginReturn = new LinkedHashMap<>();
        loginReturn.put(TokenConstant.bearerKey, TokenConstant.bearer);
        loginReturn.put(TokenConstant.accessTokenKey, accessToken);
        loginReturn.put(TokenConstant.refreshTokenKey, refreshToken);
        loginReturn.put(TokenConstant.expKey, tokenData.getAccessTokenMap().get(TokenConstant.expKey).toString());
        loginReturn.put(TokenConstant.scopesKey, tokenData.getSaveInfoMap().get(TokenConstant.scopesKey).toString());
        if (ObjUtil.isNotEmpty(absUserDetails.getExtensionResponse())) {
            loginReturn.putAll(absUserDetails.getExtensionResponse());
        }

        // 登录成功的处理
        list.forEach(loginSucProcess -> {
            if (loginSucProcess.getProcess().isExecute()) {
                loginSucProcess.execute(tokenData);
            }
        });
        return loginReturn;
    }

    @Override
    public Map<String, String> refresh(String refreshTokenStr) {
        // 获取token载荷
        Map<String, Object> payload = tokenManager.check(refreshTokenStr, true);
        Object atiObj = payload.get(TokenConstant.atiKey);

        // 判断token类型
        AssertUtils.notEmpty(atiObj, LoginException.RE_LOGIN_EXPIRED, "token无效，ati信息缺失");
        String ati = atiObj.toString();
        String jti = payload.get(TokenConstant.jtiKey).toString();

        // 校验客户端
        HttpServletRequest request = HttpServletUtils.getRequest();
        ClientDetails clientDetails = clientDetailsService.getClientDetails(request);
        // 修复：aud 在 payload 中存储为字符串，不是 HashSet
        Object audObj = payload.get(TokenConstant.audKey);
        AssertUtils.notEmpty(audObj, LoginException.RE_LOGIN_EXPIRED, "token中缺少aud信息");
        String clientIdFromToken = audObj.toString();
        AssertUtils.isTrue(Objects.equals(clientDetails.getClientId(), clientIdFromToken), LoginException.RE_LOGIN_EXPIRED, "客户端ID不匹配");

        // 获取内省数据
        Map<Object, Object> userInfo = loginUserOperationManager.getUserInfo(jti);
        AssertUtils.notEmpty(userInfo, LoginException.RE_LOGIN_EXPIRED, "token无效，找不到jti为[{}]的信息", jti);
        AssertUtils.isTrue(userInfo.containsKey(ati), LoginException.RE_LOGIN_EXPIRED, "token无效，找不到ati为[{}]的信息", ati);

        TokenData tokenData = new TokenData();
        tokenData.refresh(userInfo, jti, ati);

        // 清除旧的信息
        loginUserOperationManager.loginOut(userInfo.get(TokenConstant.userIdKey).toString(), jti);

        // 生成新的token
        String accessToken = tokenManager.create(tokenData.getAccessTokenMap());
        String refreshToken = tokenManager.create(tokenData.getRefreshTokenMap());

        // 添加返回数据
        Map<String, String> loginReturn = new LinkedHashMap<>();
        loginReturn.put(TokenConstant.bearerKey, TokenConstant.bearer);
        loginReturn.put(TokenConstant.accessTokenKey, accessToken);
        loginReturn.put(TokenConstant.refreshTokenKey, refreshToken);
        loginReturn.put(TokenConstant.expKey, tokenData.getAccessTokenMap().get(TokenConstant.expKey).toString());
        loginReturn.put(TokenConstant.scopesKey, tokenData.getSaveInfoMap().get(TokenConstant.scopesKey).toString());

        // 更新用户信息
        loginUserOperationManager.saveUserInfo(tokenData, false);
        return loginReturn;
    }

    @Override
    public void logout(String userId) {
        loginUserOperationManager.loginOut(userId);
    }

    @Override
    public void logout() {
        UserTemporary userTemporary = LoginUserUtils.getUserTemporary();
        if (clientAuthInfo.getOneLogin()) {
            logout(userTemporary.getUserId());
        } else {
            loginUserOperationManager.loginOut();
        }
    }

    /**
     * 获取用户基础信息
     *
     * @param clientDetails 客户端信息
     * @param adapter       请求参数
     * @param loginType     登录类型
     */
    private AbsUserDetails getAbsUserDetails(ClientDetails clientDetails, Object adapter, LoginTypeAdapter loginType) {
        LoginManager loginManager = loginType.getLoginManager();
        AssertUtils.notEmpty(loginManager, "登录失败！没有{}的实现", loginType.getAClass().getName());

        if (!loginManager.support(adapter)) {
            AssertUtils.error("登陆失败", "登录失败！ LoginService 实现校验失败！");
        }
        return checkUserState(loginManager.login(clientDetails, adapter));
    }

    /**
     * 校验用户状态
     *
     * @param absUserDetails 用户数据对象
     */
    protected AbsUserDetails checkUserState(AbsUserDetails absUserDetails) {
        AssertUtils.notNull(absUserDetails, "登录失败！AbsUserDetails 对象为空");
        AssertUtils.notEmpty(absUserDetails.getUserId(), "登录失败,没有这个用户", "登录失败！userId 为空");
        AssertUtils.notEmpty(absUserDetails.getLoginRole(), "登录角色不能为空");
        AssertUtils.isTrue(absUserDetails.getIsEnabled() == 1, "登录失败，账号已禁用");
        AssertUtils.isTrue(absUserDetails.getIsAccountNonExpired() == 1, "帐户已过期，请联系管理员");
        AssertUtils.isTrue(absUserDetails.getIsAccountNonLocked() == 1, "帐户已被锁定，请联系管理员");
        AssertUtils.isTrue(absUserDetails.getIsCredentialsNonExpired() == 1, "密码已经过期，请联系管理员");
        if (absUserDetails.getExtension() == null) {
            absUserDetails.setExtension("");
        }
        return absUserDetails;
    }
}