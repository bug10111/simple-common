package com.simple.common.auth.client.common.manager.token;

import cn.hutool.core.date.DateTime;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.core.utils.AssertUtils;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
public abstract class AbsTokenManager implements TokenManager {

    /**
     * 校验token是否过期
     * <p>
     * exp 缺失/非数值一律按过期处理（fail-closed），不向调用方抛出 500：
     * 校验入口必须对畸形载荷给出受控业务异常。
     * </p>
     *
     * @param payload 载荷
     * @param isRefresh 是否刷新令牌
     */
    protected void checkTime(Map<String, Object> payload, boolean isRefresh) {
        Object expObj = payload == null ? null : payload.get(TokenConstant.expKey);
        long timeOut;
        try {
            AssertUtils.notEmpty(expObj, isRefresh ? LoginException.RE_LOGIN_EXPIRED : LoginException.LOGIN_EXPIRED);
            timeOut = Long.parseLong(expObj.toString());
        } catch (NumberFormatException e) {
            AssertUtils.error(isRefresh ? LoginException.RE_LOGIN_EXPIRED : LoginException.LOGIN_EXPIRED);
            return;
        }
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
     * <p>
     * 边界语义对齐 RFC 7519：now >= exp 即视为过期（原 < 判定使 exp 恰等于当前时刻的令牌多活 1ms）。
     * </p>
     *
     * @param timeOut token的过期时间戳
     */
    protected boolean checkTime(Long timeOut) {
        return timeOut <= DateTime.now().getTime();
    }
}
