package com.simple.common.auth.client.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.eventbus.common.annotation.Event;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 * 秘钥事件
 *
 * @author qty
 */
@Data
@Event
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class SecretEvent {

    //秘钥
    private String secret;

    //操作类型
    private Operation operation;

    /**
     * 操作类型枚举
     */
    public enum Operation {
        ADD, //添加
        UPDATE, //更新
    }

}