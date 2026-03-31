package com.simple.common.auth.client.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.eventbus.common.annotation.Event;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 * Description: 秘钥变更事件（客户端接收）
 *
 * @author qty
 */
@Data
@Event
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class SecretEvent {

    /**
     * 事件类型：INIT-初始化, ADD-添加, UPDATE-更新
     */
    private String eventType;

    /**
     * 新秘钥
     */
    private String newSecret;

    /**
     * 旧秘钥（更新时使用）
     */
    private String oldSecret;
}
