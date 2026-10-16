package com.simple.common.auth.client.exchange.sync.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.simple.common.auth.client.common.enums.PermissionChangeTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * 实例同步载荷（投递引擎线上的统一契约）
 * <p>
 * 授权中心变更投递引擎的载荷本体，经投递任务队列流转、由回调客户端 POST 到各实例接收端点。
 * 单一契约按 payloadType 区分语义，仅本类型相关字段有值（序列化忽略 null 字段）：
 * SECRET → projectCode + secrets；PERMISSION → projectCode + roleKey + permissions + changeType + timestamp；
 * LOGOUT → projectCode + userId。接收端（/internal/sync/**）按类型落地各自缓存。
 * </p>
 *
 * @author qty
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
public class SyncPayload {

    /**
     * 载荷类型
     */
    private SyncPayloadType payloadType;

    /**
     * 载荷归属项目编码（即目标客户端 clientId）
     */
    private String projectCode;

    /**
     * 密钥键值（SECRET 载荷；key：jwt / sign）
     */
    private Map<String, String> secrets;

    /**
     * 角色标识（PERMISSION 载荷）
     */
    private String roleKey;

    /**
     * 角色权限键值（PERMISSION 载荷；key 为权限标识，value 为权限描述）
     */
    private Map<String, String> permissions;

    /**
     * 权限变更类型（PERMISSION 载荷；接收端按类型执行增量/全量/删除/清空缓存策略）
     */
    private PermissionChangeTypeEnum changeType;

    /**
     * 变更时间戳（PERMISSION 载荷；毫秒）
     */
    private Long timestamp;

    /**
     * 退出登录的用户 ID（LOGOUT 载荷）
     */
    private String userId;

}
