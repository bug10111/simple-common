package com.simple.common.eventbus.util;

import cn.hutool.extra.spring.SpringUtil;
import com.simple.common.eventbus.common.service.AbsEventCycleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自动重试任务工厂实现
 * <p>缓存整体替换采用「构建新表 + volatile 引用原子换指」方式，
 * 读取方（无锁快路径）任意时刻只会看到旧表或新表的完整快照，不会观察到清空中的中间态。</p>
 *
 * @author qty
 */
@Slf4j
public final class CycleFactoryUtils {

    /**
     * 服务缓存：volatile 保证换表后其他线程立即可见新快照
     */
    private static volatile Map<String, AbsEventCycleService> SERVICE_CACHE = new ConcurrentHashMap<>();
    // 初始化成功标记：成功后快路径不再加锁；失败时保持 false，后续调用继续尝试（Spring 上下文就绪后可自愈）
    private static volatile boolean initialized = false;
    // 初始化失败标记：避免上下文未就绪期间每次未命中都重复打 error 堆栈
    private static volatile boolean initFailedLogged = false;

    private CycleFactoryUtils() {
    }

    /**
     * 获取执行bean
     * <p>key 先做 trim 归一再参与校验与全部查找，避免「校验用 trim、查找用原文」导致
     * 形如 " myTask " 的合法入参静默 miss。初始化失败不锁死：保留重试入口，上下文就绪后自动装载。</p>
     *
     * @param name bean名称
     * @return AbsEventCycleService 实例，可能为null
     */
    public static AbsEventCycleService getCycleService(String name) {
        String key = name == null ? null : name.trim();
        if (key == null || key.isEmpty()) {
            log.error("Bean名称不能为空");
            return null;
        }

        // 先尝试从缓存获取
        AbsEventCycleService service = SERVICE_CACHE.get(key);
        if (service != null) {
            return service;
        }

        // 首次初始化或缓存未命中时，从Spring容器获取（失败不置成功标记，后续未命中会再次尝试）
        if (!initialized) {
            synchronized (CycleFactoryUtils.class) {
                if (!initialized) {
                    try {
                        Map<String, AbsEventCycleService> beans = SpringUtil.getBeansOfType(AbsEventCycleService.class);
                        // 原子整体替换：不做 clear+putAll，避免无锁读取方观察到清空中的中间态而漏判存活服务
                        SERVICE_CACHE = new ConcurrentHashMap<>(beans);
                        initialized = true;
                        initFailedLogged = false;
                        log.info("循环任务服务初始化完成，共加载 {} 个服务", beans.size());
                    } catch (Exception e) {
                        if (!initFailedLogged) {
                            // 上下文未就绪时每次未命中都会重入此处，error 堆栈只打一次防止刷屏；后续成功会复位标记
                            initFailedLogged = true;
                            log.error("初始化循环任务服务失败，将在后续调用时自动重试", e);
                        }
                    }
                }
            }
        }

        // 再次尝试从缓存获取
        return SERVICE_CACHE.get(key);
    }

    /**
     * 刷新服务缓存，用于动态注册场景
     * <p>构建新快照后原子替换，保证缓存与容器状态一致且切换过程对读取方无感知</p>
     */
    public static void refreshCycleServiceCache() {
        synchronized (CycleFactoryUtils.class) {
            try {
                Map<String, AbsEventCycleService> beans = SpringUtil.getBeansOfType(AbsEventCycleService.class);
                SERVICE_CACHE = new ConcurrentHashMap<>(beans);
                initialized = true;
                initFailedLogged = false;
                log.info("循环任务服务缓存已刷新，当前服务数量: {}", beans.size());
            } catch (Exception e) {
                log.error("刷新循环任务服务缓存失败", e);
            }
        }
    }

    /**
     * 容器关闭时清理静态缓存并复位初始化标记，
     * 避免上下文重启/刷新场景下向新容器交付已销毁的旧实例。
     * <p>以静态内部类形式注册为容器监听器，随事件模块组件扫描自动装配。</p>
     */
    @Component
    public static class CacheCleaner implements ApplicationListener<ContextClosedEvent> {

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            SERVICE_CACHE = new ConcurrentHashMap<>();
            initialized = false;
            initFailedLogged = false;
            log.info("容器关闭，循环任务服务缓存已清理");
        }
    }
}
