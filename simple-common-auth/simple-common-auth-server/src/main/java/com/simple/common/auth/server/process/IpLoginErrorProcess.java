package com.simple.common.auth.server.process;

import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.enums.process.LoginErrorKindProcess;
import com.simple.common.auth.server.common.process.AbsLoginErrorProcess;
import com.simple.common.core.utils.IPUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 基于IP的登录失败计数处理器
 *
 * @author qty
 */
@Slf4j
@Component
public class IpLoginErrorProcess extends AbsLoginErrorProcess {

    @Override
    protected String getLoginKey(ClientDetails clientDetails, Object adapter, String ip) {
        // 使用IP作为key
        return ip != null ? ip : IPUtils.UNKNOWN;
    }

    @Override
    protected String getKeyPrefix() {
        return authProperties.getLoginIpErrorKey();
    }

    @Override
    public LoginErrorKindProcess getProcess() {
        return LoginErrorKindProcess.IP_ERROR;
    }
}