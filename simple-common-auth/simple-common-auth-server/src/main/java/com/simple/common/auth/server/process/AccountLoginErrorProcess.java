package com.simple.common.auth.server.process;

import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.enums.process.LoginErrorKindProcess;
import com.simple.common.auth.server.common.process.AbsLoginErrorProcess;
import com.simple.common.core.utils.Base64Utils;
import com.simple.common.core.utils.CryptoUtil;
import com.simple.common.core.utils.IPUtils;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 基于账号的登录错误计数处理器
 *
 * @author qty
 */
@Component
public class AccountLoginErrorProcess extends AbsLoginErrorProcess {

    @Override
    public LoginErrorKindProcess getProcess() {
        return LoginErrorKindProcess.ACCOUNT_ERROR;
    }

    @Override
    protected String getLoginKey(ClientDetails clientDetails, Object adapter, String ip) {
        return adapter != null ? Base64Utils.encode(CryptoUtil.hash(CryptoUtil.HashAlgorithmType.MD5, adapter.toString().getBytes())) : IPUtils.UNKNOWN;
    }

    @Override
    protected String getKeyPrefix() {
        return authProperties.getLoginErrorKey();
    }
}