package com.simple.common.auth.client.common.annotation;

import java.lang.annotation.*;

/**
 * Created with IntelliJ IDEA
 * Description: 权限注解
 *
 * @author 兄台丶请冷静
 */
@Documented
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface HasAuthority {

    // 权限值
    String[] value();

}
