package com.simple.common.eventbus.util;

import cn.hutool.extra.spring.SpringUtil;
import com.simple.common.eventbus.common.service.AbsEventCycleService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自动重试任务工厂实现
 *
 * @author qty
 */
@Slf4j
public final class CycleFactoryUtils {

    private static final Map<String, AbsEventCycleService> SERVICE_CACHE = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    private CycleFactoryUtils() {
    }

    /**
     * 获取执行bean
     *
     * @param name bean名称
     * @return AbsEventCycleService 实例，可能为null
     */
    public static AbsEventCycleService getCycleService(String name) {
        if (name == null || name.trim().isEmpty()) {
            log.error("Bean名称不能为空");
            return null;
        }

        // 先尝试从缓存获取
        AbsEventCycleService service = SERVICE_CACHE.get(name);
        if (service != null) {
            return service;
        }

        // 首次初始化或缓存未命中时，从Spring容器获取
        if (!initialized) {
            synchronized (CycleFactoryUtils.class) {
                if (!initialized) {
                    try {
                        Map<String, AbsEventCycleService> beans = SpringUtil.getBeansOfType(AbsEventCycleService.class);
                        SERVICE_CACHE.putAll(beans);
                        initialized = true;
                        log.info("循环任务服务初始化完成，共加载 {} 个服务", beans.size());
                    } catch (Exception e) {
                        log.error("初始化循环任务服务失败", e);
                    }
                }
            }
        }

        // 再次尝试从缓存获取
        return SERVICE_CACHE.get(name);
    }

    /**
     * 刷新服务缓存，用于动态注册场景
     */
    public static void refreshServiceCache() {
        synchronized (CycleFactoryUtils.class) {
            try {
                Map<String, AbsEventCycleService> beans = SpringUtil.getBeansOfType(AbsEventCycleService.class);
                SERVICE_CACHE.clear();
                SERVICE_CACHE.putAll(beans);
                initialized = true;
                log.info("循环任务服务缓存已刷新，共加载 {} 个服务", beans.size());
            } catch (Exception e) {
                log.error("刷新循环任务服务缓存失败", e);
            }
        }
    }
}