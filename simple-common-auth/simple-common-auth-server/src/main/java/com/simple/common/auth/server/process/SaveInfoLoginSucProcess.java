package com.simple.common.auth.server.process;

import com.simple.common.auth.server.common.entity.TokenData;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.server.common.enums.process.LoginSucKindProcess;
import com.simple.common.auth.server.common.manager.user.LoginUserOperationManager;
import com.simple.common.auth.server.common.process.LoginSucProcess;
import com.simple.common.core.common.enums.process.DefaultKindProcess;
import com.simple.common.core.utils.AssertUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 保存用户登录信息
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
        // 入参契约断言：本处理器是登录成功链路的一环，tokenData 缺失属调用方缺陷，
        // 快速失败并携带处理器语义，避免在缓存管理器深处抛出无指向性的 NPE
        AssertUtils.notNull(tokenData, "登录成功保存用户信息时 tokenData 不能为空");
        try {
            loginUserOperationManager.saveUserInfo(tokenData, true);
        } catch (Exception e) {
            // 保存失败记日志后原样抛出：登录已在 DefaultLoginService 生成令牌，
            // 静默吞掉会让客户端拿到 token 而服务端无内省数据（刷新/登出全部失灵）
            log.error("保存用户登录信息失败: userId={}", tokenData.getSaveInfoMap().get(TokenConstant.userIdKey), e);
            throw e;
        }
    }

    @Override
    public DefaultKindProcess getProcess() {
        return LoginSucKindProcess.SAVE_INFO;
    }
}
