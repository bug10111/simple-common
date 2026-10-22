package com.simple.common.auth.client.init;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.core.common.enums.order.SimpleOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA
 * 客户端权限信息初始化。
 * <p>
 * 幂等保护：ApplicationReadyEvent 可能因上下文刷新/多容器/子容器事件重复触达，
 * 无幂等护栏时二次执行会把已反转的 url->roles 再反转回 role->urls，
 * byRoleMap 查询恒 miss，roleByRestricted 限制静默失效（fail-open 越权）。
 * 角色映射经 {@link ClientAuthInfo#setByRoleMap(Map)} 整体引用替换，运行期读者只见完整新/旧态。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class AuthInit implements ApplicationListener<ApplicationReadyEvent>, Ordered {

    /**
     * 幂等标记：事件只处理一次
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 幂等护栏：仅本实例首次事件生效，重复事件（刷新/子容器）直接忽略
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        //角色允许的路由
        Map<String, Set<String>> byRoleMap = clientAuthInfo.getByRoleMap();
        Map<String, Set<String>> byRoleMapNew = new java.util.HashMap<>();

        //处理角色允许的路由
        processByRoleMap(byRoleMap, byRoleMapNew);

        //数据缓存（整体替换，原子发布）
        clientAuthInfo.setByRoleMap(byRoleMapNew);
        log.info("权限信息初始化完成");
    }

    /**
     * 处理数据
     *
     * @param map    原数据
     * @param mapNew 处理后的数据
     */
    private void processByRoleMap(Map<String, Set<String>> map, Map<String, Set<String>> mapNew) {
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            Set<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
                values.forEach(url -> {
                    mapNew.computeIfAbsent(url, k -> new java.util.HashSet<>()).add(entry.getKey());
                });
            }
        }
    }

    @Override
    public int getOrder() {
        return SimpleOrder.Auth.getOrder();
    }
}
