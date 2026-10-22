package com.simple.common.auth.server.common.adapter;

import cn.hutool.extra.spring.SpringUtil;
import com.simple.common.auth.server.common.manager.login.LoginManager;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
public interface LoginTypeAdapter {

    /**
     * 获取该登录类型绑定的 LoginManager 实现类型。
     *
     * @return 登录类型对应的 LoginManager 实现类型
     * @deprecated 语义不明易误用，请实现/覆写 {@link #getLoginManagerClass()}；
     * 本方法保留默认桥接以兼容既有实现
     */
    /**
     * 获取该登录类型绑定的 LoginManager 实现类型。
     * <p>
     * 契约：返回值不能为 {@code null}，且必须为已注册为 Spring Bean 的类，
     * 供 {@link SpringUtil#getBean(Class)} 按类型解析。
     * </p>
     *
     * @return 登录类型对应的 LoginManager 实现类型
     * @deprecated 命名语义不明易误用，请改为覆写 {@link #getLoginManagerClass()}；
     * 框架解析经 {@link #getLoginManagerClass()}，覆写后本方法不再被消费
     */
    @Deprecated
    Class<? extends LoginManager> getAClass();

    /**
     * 获取该登录类型绑定的 LoginManager 实现类型（推荐覆写本方法）。
     * <p>
     * 契约：返回值不能为 {@code null}，且必须为已注册为 Spring Bean 的类，
     * 供 {@link SpringUtil#getBean(Class)} 按类型解析。
     * 缺省桥接到 {@link #getAClass()} 以兼容既有实现。
     * </p>
     *
     * @return 登录类型对应的 LoginManager 实现类型
     */
    default Class<? extends LoginManager> getLoginManagerClass() {
        return getAClass();
    }

    /**
     * 解析该登录类型对应的 LoginManager Bean。
     * <p>
     * getLoginManagerClass() 返回 {@code null} 时以描述性异常立即失败，避免落入不透明的
     * NPE/IllegalArgumentException；注意 {@code getBean(Class)} 本身不会返回 null：
     * 无 Bean 时抛 NoSuchBeanDefinitionException、存在多个同类型 Bean 时抛
     * NoUniqueBeanDefinitionException，调用方按需捕获处理。
     * </p>
     *
     * @return 登录类型对应的 LoginManager Bean（非 null，由 Spring 解析保证）
     * @throws IllegalStateException getLoginManagerClass() 返回 null 时抛出
     */
    default LoginManager getLoginManager() {
        Class<? extends LoginManager> managerClass = getLoginManagerClass();
        if (managerClass == null) {
            throw new IllegalStateException("LoginTypeAdapter [" + getClass().getName()
                    + "] 未声明 LoginManager 实现类型（getLoginManagerClass 返回 null）");
        }
        return SpringUtil.getBean(managerClass);
    }
}
