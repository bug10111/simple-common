package com.simple.common.core.common.aspect;

import com.simple.common.core.common.xss.XssValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Created with IntelliJ IDEA
 * Description: 扩展Validation检验XSS标签
 * <p>契约：仅支持 {@link String} 类型目标（{@link com.simple.common.core.common.xss.XssValidator} 只校验字符串），
 * {@code null} 视为合法（由其它非空校验负责）；不支持集合/容器元素级校验。</p>
 *
 * @author qty
 */
@Documented
@Constraint(validatedBy = XssValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface XssSafe {
    String message() default "含有HTML标签，不允许提交";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}