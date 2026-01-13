package com.simple.common.auth.client.common.annotation;

import java.lang.annotation.*;

/**
 * Created with IntelliJ IDEA
 * Description: CSRF防御注解，同时也能防重复提交
 *
 * @author qty
 */
@Documented
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CsrfDefense {

}
