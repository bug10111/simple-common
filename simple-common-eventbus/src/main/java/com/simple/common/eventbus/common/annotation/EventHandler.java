package com.simple.common.eventbus.common.annotation;

import com.simple.common.eventbus.common.constants.EventConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created with IntelliJ IDEA
 * Description: 事件监听，标记的方法只能允许一个参数，且参数对象必须添加注解@Event（事件类标记），
 *               来标记这个对象是一个事件。
 *               注意：注册时仅校验方法只能有一个参数；@Event 注解在事件发布侧（EventBusService.push）校验，
 *               注册侧不校验参数类型是否带 @Event（缺少该注解的事件无法被发布，处理器自然不会被触发）。
 *
 * @author qty
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {

    /**
     * 事件名称，未指定时，取事件类的短类名
     */
    String value() default "";

    /**
     * 表示要处理的事件来源系统，一般发送到本系统的事件只有所有系统和自己，默认接收当前系统的事件。
     * <p>注意：必须至少声明一个元素；空数组 {@code {}} 会导致注册时校验失败（快速失败），不会静默忽略。
     * 元素支持 {@link com.simple.common.eventbus.common.constants.EventConstant#THIS_MACHINE} 占位符，
     * 注册时解析为当前应用名。</p>
     */
    String[] applicationName() default EventConstant.THIS_MACHINE;
}
