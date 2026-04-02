package com.simple.common.auth.server.common.event;

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
public class SecretEvent {

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

    /**
     * 操作类型枚举
     */
    public enum Operation {
        ADD, //添加
        UPDATE, //更新
        DELETE //删除
    }

}