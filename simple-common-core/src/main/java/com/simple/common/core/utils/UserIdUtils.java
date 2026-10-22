package com.simple.common.core.utils;

import cn.hutool.extra.spring.SpringUtil;
import com.simple.common.core.common.service.jwt.CoreLoginUserService;

/**
 * Created with IntelliJ IDEA
 * Description: 全局获取userId帮助类
 * <p>契约：用户未登录/当前线程无用户上下文时返回 null（跟随 {@link CoreLoginUserService#getUserId()} 的统一契约，
 * 不抛异常）；调用方按需判空。容器未初始化时以明确IllegalStateException快速失败，而非裸NoSuchBeanDefinitionException。</p>
 *
 * @author qty
 */
public class UserIdUtils {

    private UserIdUtils() {
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID；未登录/无用户上下文时返回null
     * @throws IllegalStateException Spring容器未初始化或未注册CoreLoginUserService实现时抛出
     */
    public static String getUserId() {
        CoreLoginUserService service;
        try {
            service = SpringUtil.getBean(CoreLoginUserService.class);
        } catch (Exception e) {
            // bean缺失/容器未刷新在此处翻译为可定位错误，原AssertUtils.notEmpty对getBean异常是死代码
            throw new IllegalStateException("CoreLoginUserService 未注册或Spring容器未就绪，无法获取当前用户", e);
        }
        return service.getUserId();
    }
}
