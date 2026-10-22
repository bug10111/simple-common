package com.simple.common.auth.client.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created with IntelliJ IDEA
 * Description: 签名注解
 * <p>
 * 签名消息 = 业务参数串 + timestamp + nonce + userId + method + path（SignAspect 契约），
 * 客户端构造签名串须与服务端一致。排除字段（excludeFields）不参与完整性校验，
 * 排除即脱离篡改保护，敏感字段以外的业务字段禁止排除。
 * </p>
 *
 * @author qty
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sign {

    /**
     * 是否启用签名校验，默认为 {@code true}；置为 {@code false} 时该接口跳过全部签名校验
     * （不受请求方式限制，也不会消耗 nonce）
     */
    boolean enabled() default true;

    /**
     * 参与签名的字段中需要排除的字段名（例如敏感字段）。
     * 注意：被排除的字段脱离完整性保护，改动其值不影响验签结果
     */
    String[] excludeFields() default {};

    /**
     * 是否需要校验时间戳（时效性），默认 {@code true}；关闭后时间窗口保护失效，仅剩 nonce 防重放
     */
    boolean checkTimestamp() default true;

    /**
     * 是否需要校验 nonce（防重放），默认 {@code true}；关闭后重放保护仅剩时间窗口
     */
    boolean checkNonce() default true;
}
