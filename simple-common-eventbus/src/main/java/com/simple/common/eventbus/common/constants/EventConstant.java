package com.simple.common.eventbus.common.constants;

import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 定义事件默认常量
 *
 * @author 兄台丶请冷静
 */
@Component
public class EventConstant {

    //所有系统
    public static final String TARGET_ALL_X = "event.all";

    //本系统
    public static final String THIS_MACHINE = "event.this";
}
