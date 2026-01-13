package com.simple.common.auth.client.common.annotation;

import java.lang.annotation.*;

/**
 * Created with IntelliJ IDEA
 * Description: 解密注解
 *
 * @author qty
 */
@Documented
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Decryption {

    //指定加密某个参数
    String[] value() default {};

    //是否验签
    boolean sign() default false;
}
