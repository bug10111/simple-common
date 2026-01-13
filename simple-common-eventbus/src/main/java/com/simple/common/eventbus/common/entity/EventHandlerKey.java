package com.simple.common.eventbus.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(of = { "applicationName", "eventName" })//重写equals和HashCode方法，用于对比key是否相等
@Accessors(chain = true)
public final class EventHandlerKey {

    //系统名称
    private String applicationName;

    //事件名称
    private String eventName;

}
