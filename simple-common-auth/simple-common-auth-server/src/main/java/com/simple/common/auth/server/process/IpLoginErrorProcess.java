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
        // 使用“客户端:IP”作为 key（不同客户端的失败计数互不影响，避免 NAT 后不同客户端的用户互相误伤）
        // 空白/未知 IP 统一归一；返回 null 表示不参与计数（IP 未知时不聚合到公共计数器，避免内部登录与外部攻击互相锁定）
        if (ip == null || ip.trim().isEmpty() || IPUtils.UNKNOWN.equalsIgnoreCase(ip.trim())) {
            return null;
        }
        String clientKey = clientDetails != null ? clientDetails.getClientId() : IPUtils.UNKNOWN;
        return clientKey + ":" + ip.trim();
    }

    @Override
    public LoginErrorKindProcess getProcess() {
        return LoginErrorKindProcess.IP_ERROR;
    }
}