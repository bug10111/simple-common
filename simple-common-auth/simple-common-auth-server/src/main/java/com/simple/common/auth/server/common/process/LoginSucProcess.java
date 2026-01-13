package com.simple.common.auth.server.common.process;

import com.simple.common.auth.server.common.entity.TokenData;
import com.simple.common.core.common.service.process.BasProcessService;

/**
 * Created with IntelliJ IDEA
 * Description: 登录成功后需要执行的事情
 *
 * @author qty
 */
public interface LoginSucProcess extends BasProcessService {

    /**
     * 执行权限过滤器
     *
     * @param tokenData 登录中token数据对象
     */
    void execute(TokenData tokenData);
}
