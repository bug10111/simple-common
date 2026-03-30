package com.simple.common.auth.client.common.annotation;

import java.lang.annotation.*;

/**
 * Created with IntelliJ IDEA
 * Description: 签名注解
 *
 * @author qty
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sign {

    //是否启用签名校验，默认为 true
    boolean enabled() default true;

    //参与签名的字段中需要排除的字段名（例如敏感字段）
    String[] excludeFields() default {};

    //是否需要校验时间戳（时效性），默认 true
    boolean checkTimestamp() default true;

    //是否需要校验 nonce（防重放），默认 true
    boolean checkNonce() default true;
}
