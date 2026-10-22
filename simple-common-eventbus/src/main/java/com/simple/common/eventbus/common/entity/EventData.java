package com.simple.common.eventbus.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.simple.common.rabbitmq.common.entity.DefaultMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: 发送的事件消息体
 *
 * @author qty
 */
@Data
// 父类 DefaultMessage 未覆写 equals/hashCode（仍是 Object 身份语义），callSuper=true 会使等值判断失效；仅按本类字段判等
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
public class EventData extends DefaultMessage {

    //发送事件的系统名称
    private String applicationName;

    //发送的事件名称
    private String eventName;

    //事件的目标服务（默认空列表，保证手工构造或 JSON 缺字段的场景下消费侧可直接遍历，不会 NPE）
    private List<String> objectivesApplication = new ArrayList<>();

    //注意：@Accessors(chain = true) 仅对本类声明的字段生效；继承自 DefaultMessage 的 setter 仍返回 DefaultMessage，
    //链式调用时本类字段与父类字段（msgData/sendTime/id）不可混在同一条链上
}
