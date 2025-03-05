package com.simple.common.auth.client.common.manager.token;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.core.utils.AssertUtils;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
public abstract class AbsTokenManager implements TokenManager {

    /**
     * 校验token是否过期
     *
     * @param payload 载荷
     */
    protected void checkTime(Map<String, Object> payload, boolean isRefresh) {
        Long timeOut = Long.parseLong(payload.get(TokenConstant.expKey).toString());
        if (checkTime(timeOut)) {
            if (isRefresh) {
                AssertUtils.error(LoginException.RE_LOGIN_EXPIRED);
            } else {
                AssertUtils.error(LoginException.LOGIN_EXPIRED);
            }
        }
    }

    /**
     * 校验token是否过期
     *
     * @param timeOut token的过期时间戳
     */
    protected boolean checkTime(Long timeOut) {
        return timeOut < DateTime.now().getTime();
    }
}
