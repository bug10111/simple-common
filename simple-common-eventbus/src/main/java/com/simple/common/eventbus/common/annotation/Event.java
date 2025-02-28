package com.simple.common.eventbus.common.annotation;

import com.simple.common.eventbus.common.constants.EventConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created with IntelliJ IDEA
 * Description: 事件标记
 *
 * @author 兄台丶请冷静
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Event {

    /**
     * 事件名称，未指定时，取事件类的短类名
     */
    String value() default "";

    /**
     * 要接收该事件的全部目标系统名称,在中间件实现中使用，同步事件无效
     */
    String[] targets() default EventConstant.THIS_MACHINE;
}
