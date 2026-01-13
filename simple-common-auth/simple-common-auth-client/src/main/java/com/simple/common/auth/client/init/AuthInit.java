package com.simple.common.auth.client.init;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.core.common.enums.order.SimpleOrder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;

/**
 * Created with IntelliJ IDEA
 * 客户端权限信息初始化
 *
 * @author qty
 */
@Slf4j
@Component
public class AuthInit implements ApplicationListener<ApplicationReadyEvent>, Ordered {

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        //角色允许的路由
        Map<String, HashSet<String>> byRoleMap = clientAuthInfo.getByRoleMap();
        Map<String, HashSet<String>> byRoleMapNew = new HashMap<>();

        //处理角色允许的路由
        processing(byRoleMap, byRoleMapNew);

        //数据缓存
        clientAuthInfo.setByRoleMap(byRoleMapNew);
        log.info("权限信息初始化完成");
    }

    /**
     * 处理数据
     *
     * @param map    原数据
     * @param mapNew 处理后的数据
     */
    private void processing(Map<String, HashSet<String>> map, Map<String, HashSet<String>> mapNew) {
        for (String str : map.keySet()) {
            Set<String> values = map.get(str);
            if (!values.isEmpty()) {
                values.forEach(s -> {
                    mapNew.computeIfAbsent(s, k -> new HashSet<>()).add(str);
                });
            }
        }
    }

    @Override
    public int getOrder() {
        return SimpleOrder.Auth.getOrder();
    }
}
