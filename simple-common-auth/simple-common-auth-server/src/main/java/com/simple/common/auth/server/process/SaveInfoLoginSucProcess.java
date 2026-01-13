package com.simple.common.auth.server.process;

import com.simple.common.auth.server.common.entity.TokenData;
import com.simple.common.auth.server.common.enums.process.LoginSucKindProcess;
import com.simple.common.auth.server.common.manager.user.LoginUserOperationManager;
import com.simple.common.auth.server.common.process.LoginSucProcess;
import com.simple.common.core.common.enums.process.DefaultKindProcess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 登陆成功日志记录
 *
 * @author qty
 */
@Slf4j
@Component
public class SaveInfoLoginSucProcess implements LoginSucProcess {

    @Autowired
    private LoginUserOperationManager loginUserOperationManager;

    @Override
    public void execute(TokenData tokenData) {
        loginUserOperationManager.saveUserInfo(tokenData, true);
    }

    @Override
    public DefaultKindProcess getProcess() {
        return LoginSucKindProcess.SAVE_INFO;
    }
}
