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
 * @author qty
 */
@Data
@Event
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class CycleEvent {

    //执行的bean
    private String beanName;

    /**
     * 参数对象
     * <p>JSON 往返契约：仅支持可 JSON 序列化的标量/集合类型（字符串、数字、日期用时间戳或标准格式、Map/List）。
     * 经 MQ/延迟消息传输后反序列化得到的是 LinkedHashMap 等通用容器，目标类型还原由
     * AbsEventCycleService.toBean 按 getEventClass() 转换完成，禁止假设其为原始强类型。</p>
     */
    private Object runBody;

    //总执行次数（含首次）
    private Integer sum = 1;

    //当前次数
    private Integer num = 0;

    //周期的单位时间间隔
    private Integer timeInterval = 0;

    //是否累加
    private Boolean isAccumulate = Boolean.FALSE;

    //扩展参数
    private Map<String, Object> reserve;
}
