package com.simple.common.core.common.aspect;

import com.simple.common.core.common.xss.XssValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Created with IntelliJ IDEA
 * Description: 扩展Validation检验XSS标签
 *
 * @author qty
 */
@Documented
@Constraint(validatedBy = XssValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface XssSafe {
    String message() default "含有HTML标签，不允许提交";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}