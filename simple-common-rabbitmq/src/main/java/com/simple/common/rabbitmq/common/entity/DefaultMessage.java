package com.simple.common.rabbitmq.common.entity;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 * Description: 消息队列消息载体
 *
 * @author qty
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
public class DefaultMessage {

    //发送的事件对象json
    private String msgData;

    //发送的时间
    private String sendTime;

    //消息唯一id
    private String id;

    public void initialization() {
        if (StrUtil.isEmpty(id)) {
            this.id = IdUtil.getSnowflakeNextIdStr();
        }
        this.sendTime = DateUtil.date().toString();
    }
}