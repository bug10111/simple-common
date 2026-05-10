package com.simple.common.auth.server.common.event;

import com.simple.common.eventbus.common.annotation.Event;
import com.simple.common.eventbus.common.constants.EventConstant;
import lombok.Getter;
import lombok.Setter;

/**
 * Created with IntelliJ IDEA
 * 秘钥事件
 *
 * @author qty
 */
@Getter
@Setter
@Event(targets = EventConstant.TARGET_ALL_X)
public class SecretEvent {

    //项目编码（spring.application.name），用于标识密钥所属项目
    private String projectCode;

    //客户端ID（可选，用于客户端级别的秘钥管理）
    private String clientId;

    //秘钥
    private String secret;

    //旧秘钥（用于更新操作）
    private String oldSecret;

    //新秘钥
    private String newSecret;

    //操作类型
    private Operation operation;

    //事件类型（兼容旧代码）
    private String eventType;

    //密钥类型（JWT或SIGN）
    private SecretType secretType;

    /**
     * 操作类型枚举
     */
    public enum Operation {
        ADD, //添加
        UPDATE, //更新
        DELETE //删除
    }

    /**
     * 密钥类型枚举
     */
    public enum SecretType {
        JWT,   // JWT签名密钥
        SIGN   // API签名密钥
    }

}