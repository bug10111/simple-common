package com.simple.common.auth.server.process;

import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.server.common.entity.TokenData;
import com.simple.common.auth.server.common.enums.process.LoginSucKindProcess;
import com.simple.common.auth.server.common.process.LoginSucProcess;
import com.simple.common.core.common.enums.process.DefaultKindProcess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 登陆成功日志记录
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Component
public class LogLoginSucProcess implements LoginSucProcess {

    @Override
    public void execute(TokenData tokenData) {
        if(log.isDebugEnabled()){
            log.debug("用户[{}]登录成功！", tokenData.getSaveInfoMap().get(TokenConstant.userIdKey));
        }
    }

    @Override
    public DefaultKindProcess getProcess() {
        return LoginSucKindProcess.LOGIN_LOG;
    }
}
