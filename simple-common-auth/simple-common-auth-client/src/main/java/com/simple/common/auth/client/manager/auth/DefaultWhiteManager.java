package com.simple.common.auth.client.manager.auth;

import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.manager.auth.WhiteManager;
import com.simple.common.core.utils.UrlRulesUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA
 * 默认 IP 白名单管理器。
 * <p>
 * 语义：任一匹配当前路径的 IP 限制规则包含请求 IP，即放行（多规则并集放行，
 * 与 UrlRulesUtils 的迭代顺序解耦，规则命中结果确定）。
 * </p>
 *
 * @author qty
 */
@Component
public class DefaultWhiteManager implements WhiteManager {

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Override
    public boolean checkWhiteIp(String path, String ipAddr) {

        //获取需要限制IP的路由（entrySet 同源读取，避免 keySet+get 跨并发修改的二次查找）
        Map<String, Set<String>> ipMap = clientAuthInfo.getIpMap();

        //有ip限制
        if (ObjUtil.isNotEmpty(ipMap)) {

            for (Map.Entry<String, Set<String>> entry : ipMap.entrySet()) {

                //当前请求处于限制范围
                if (UrlRulesUtils.isMatch(entry.getKey(), path)) {

                    //获取允许的IP地址
                    Set<String> allowIps = entry.getValue();

                    //命中任一规则的允许IP即放行；不包含则继续检查后续规则（多规则并集）
                    if (ObjUtil.isNotEmpty(allowIps) && allowIps.contains(ipAddr)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
