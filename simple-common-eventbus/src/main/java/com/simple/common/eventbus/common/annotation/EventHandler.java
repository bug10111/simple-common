package com.simple.common.eventbus.common.annotation;

import com.simple.common.eventbus.common.constants.EventConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created with IntelliJ IDEA
 * Description: 事件监听，标记的方法只能允许一个参数，切参数对象必须添加注解@Event,来标记这个对象是一个事件
 *
 * @author 兄台丶请冷静
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {

    /**
     * 表示要处理的事件名称，事件名称指定后，系统会将事件的数据，反序列化为参数类型。未指定时，取事件类的短类名
     */
    String value() default "";

    /**
     * 表示要处理到哪的事件，一般发送到本系统的事件只有所有和自己，默认接收当前系统数据
     */
    String[] applicationName() default EventConstant.THIS_MACHINE;
}
