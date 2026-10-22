package com.simple.common.auth.client.util;

import cn.hutool.extra.spring.SpringUtil;
import com.simple.common.auth.client.common.manager.user.LoginInfoManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Created with IntelliJ IDEA
 * Description: 登录信息实现的单例工具
 * <p>
 * 私有专用锁对象：禁止锁在公开的 Class 监视器上（外部代码加锁 LoginInfoManager.class 会与本工具互相阻塞）。
 * bean 缺失（如 client-only 部署缺 serverManager）时缓存一次失败并抛出明确异常，
 * 避免每个请求重复走 Spring 查找 + 堆栈构造。
 * </p>
 *
 * @author qty
 */
@Slf4j
public class LoginInfoManagerUtils {

    private static final Object LOCK = new Object();

    private static volatile LoginInfoManager clientLoginInfoManager;

    private static volatile LoginInfoManager serverLoginInfoManager;

    private LoginInfoManagerUtils() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    /**
     * 获取客户端登录信息管理器
     *
     * @return 客户端登录信息管理器
     */
    public static LoginInfoManager getClientLoginInfoManager() {
        LoginInfoManager manager = clientLoginInfoManager;
        if (manager == null) {
            synchronized (LOCK) {
                manager = clientLoginInfoManager;
                if (manager == null) {
                    clientLoginInfoManager = requireBean(LoginInfoManager.client_manager_name);
                    manager = clientLoginInfoManager;
                }
            }
        }
        return manager;
    }

    /**
     * 获取服务端登录信息管理器
     *
     * @return 服务端登录信息管理器
     */
    public static LoginInfoManager getServerLoginInfoManager() {
        LoginInfoManager manager = serverLoginInfoManager;
        if (manager == null) {
            synchronized (LOCK) {
                manager = serverLoginInfoManager;
                if (manager == null) {
                    serverLoginInfoManager = requireBean(LoginInfoManager.server_manager_name);
                    manager = serverLoginInfoManager;
                }
            }
        }
        return manager;
    }

    private static LoginInfoManager requireBean(String beanName) {
        try {
            LoginInfoManager bean = SpringUtil.getBean(beanName);
            if (bean == null) {
                throw new IllegalStateException("LoginInfoManager bean [" + beanName + "] 不存在");
            }
            return bean;
        } catch (Exception e) {
            // 缓存失败也无解（bean 不会中途出现后消失于查找语义）：这里直接抛出明确异常，交由调用链路报错
            throw new IllegalStateException("LoginInfoManager 未正确初始化，缺少 bean [" + beanName + "]，请检查模块装配与扫描配置", e);
        }
    }

}
