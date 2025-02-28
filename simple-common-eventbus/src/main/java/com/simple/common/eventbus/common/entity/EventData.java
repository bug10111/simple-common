package com.simple.common.eventbus.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.simple.common.rabbitmq.common.config.DefaultMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: 发送的事件消息体
 *
 * @author 兄台丶请冷静
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
public class EventData extends DefaultMessage {

    //发送事件的系统名称
    private String applicationName;

    //发送的事件名称
    private String eventName;

    //事件的目标服务
    private List<String> objectivesApplication;

    //冗余：当前本条消息的发送目标
    private String objectives;
}
