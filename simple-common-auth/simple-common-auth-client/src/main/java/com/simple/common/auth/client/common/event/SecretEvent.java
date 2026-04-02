package com.simple.common.auth.client.common.event;

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

    //操作类型
    private Operation operation;

    /**
     * 操作类型枚举
     */
    public enum Operation {
        ADD, //添加
        UPDATE, //更新
        DELETE //删除
    }

}