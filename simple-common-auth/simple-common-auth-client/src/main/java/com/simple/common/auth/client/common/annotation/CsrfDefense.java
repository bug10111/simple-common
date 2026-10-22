package com.simple.common.auth.client.common.annotation;

import java.lang.annotation.*;

/**
 * CSRF防御注解，同时具备防重复提交能力。
 * <p>
 * Token 契约：Token 从 {@code CsrfProperties#csrfHeader}（默认 X-CSRF-TOKEN）请求头读取，
 * 校验对象为「当前请求 URI + 登录 userId」维度的缓存值；校验失败立即拒绝请求，不存在静默不校验。
 * 防重复提交仅在 {@code consume=true}（默认）时成立：校验通过即原子删除 Token。
 * </p>
 *
 * @author qty
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CsrfDefense {

    /**
     * 校验后是否立即删除 token（即 token 是一次性的）。
     * 默认为 true，保持向后兼容。
     * 若为 false，token 在 {@code CsrfProperties#cacheTime} TTL 内（默认30分钟）按
     * 「userId + path」维度可重复使用直至过期——此形态只适用于幂等/非最终提交的校验步骤，
     * 重复提交窗口即 cacheTime TTL；最终提交类接口必须保持默认 true。
     */
    boolean consume() default true;

}