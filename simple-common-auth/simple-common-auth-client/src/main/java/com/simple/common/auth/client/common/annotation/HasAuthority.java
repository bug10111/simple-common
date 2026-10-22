package com.simple.common.auth.client.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created with IntelliJ IDEA
 * Description: 权限注解
 * <p>
 * 匹配语义为 <b>ANY-of</b>：value 数组中任一权限命中即通过；
 * 空数组/空白值视为配置错误，HasAuthorityAspect 直接按权限不足拒绝（禁止隐式放行）。
 * 仅当登录校验与鉴权开关同时开启时生效（ClientAuthInfo#openLogin + openAuthentication）。
 * </p>
 *
 * @author qty
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface HasAuthority {

    /**
     * 所需权限值，多个值之间为「任一满足即通过」（ANY-of）语义。
     * <p>不允许为空数组或空白字符串，否则视为配置错误（拒绝访问）。</p>
     */
    String[] value();

}
