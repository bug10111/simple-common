package com.simple.common.auth.client.manager.auth;

import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.manager.auth.WhiteManager;
import com.simple.common.core.utils.UrlRulesUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultWhiteManager implements WhiteManager {

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Override
    public boolean checkWhiteIp(String path, String ipAddr) {

        //获取需要限制IP的路由
        Set<String> paths = clientAuthInfo.getIpMap().keySet();

        //有ip限制
        if (ObjUtil.isNotEmpty(paths)) {

            for (String s : paths) {

                //当前请求处于限制范围
                if (UrlRulesUtils.isMatch(s, path)) {

                    //获取允许的IP地址
                    HashSet<String> strings = clientAuthInfo.getIpMap().get(s);

                    //不包含，不允许通行
                    return !ObjUtil.isEmpty(strings) && strings.contains(ipAddr);
                }
            }
        }
        return false;
    }
}