package com.simple.common.auth.server.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.eventbus.common.annotation.Event;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 * Description: 秘钥变更事件
 *
 * @author qty
 */
@Data
@Event
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class SecretEvent {

    /**
     * 秘钥操作类型
     */
    private String eventType;

    /**
     * 新秘钥
     */
    private String newSecret;

    /**
     * 旧秘钥（仅修改操作时有值）
     */
    private String oldSecret;
}