package com.simple.common.eventbus.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.eventbus.common.annotation.Event;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 自动重试任务的发布事件
 *
 * @author 兄台丶请冷静
 */
@Data
@Event
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class CycleEvent {

    //执行的bean
    private String beanName;

    //参数对象
    private Object runBody;

    //总执行次数
    private Integer sum;

    //当前次数
    private Integer num;

    //周期的单位时间间隔
    private Integer timeInterval;

    //是否累加
    private Boolean isAccumulate;

    //扩展参数
    private Map<String, Object> reserve;
}
