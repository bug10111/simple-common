package com.simple.common.auth.client.common.annotation;

import java.lang.annotation.*;

/**
 * Created with IntelliJ IDEA
 * Description: CSRF防御注解，同时也能防重复提交
 *
 * @author 兄台丶请冷静
 */
@Documented
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CsrfDefense {

}
