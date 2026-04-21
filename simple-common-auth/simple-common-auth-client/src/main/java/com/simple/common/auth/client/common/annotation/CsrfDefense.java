package com.simple.common.auth.client.common.annotation;

import java.lang.annotation.*;

/**
 * CSRF防御注解，同时也能防重复提交。
 *
 * @author qty
 */
@Documented
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CsrfDefense {

    /**
     * 校验后是否立即删除 token（即 token 是一次性的）。
     * 默认为 true，保持向后兼容。
     * 若为 false，token 可重复使用直至过期，适用于非最终提交的校验场景。
     */
    boolean consume() default true;

}