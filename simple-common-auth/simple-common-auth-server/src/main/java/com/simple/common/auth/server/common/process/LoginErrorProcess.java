package com.simple.common.auth.server.common.process;

import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.enums.process.LoginErrorKindProcess;
import com.simple.common.core.common.service.process.BasProcessService;

/**
 * Created with IntelliJ IDEA
 * Description: 登录异常处理接口（责任链模式）
 *
 * @author qty
 */
public interface LoginErrorProcess extends BasProcessService {

    /**
     * 检查登录失败次数是否超限
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     * @param ip            请求IP
     * @return true-通过校验，false-不通过
     */
    boolean checkErrorNum(ClientDetails clientDetails, Object adapter, String ip);

    /**
     * 记录登录失败
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     * @param ip            请求IP
     */
    void recordError(ClientDetails clientDetails, Object adapter, String ip);

    /**
     * 清除登录失败记录（登录成功后调用）
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     * @param ip            请求IP
     */
    void clearError(ClientDetails clientDetails, Object adapter, String ip);

}
