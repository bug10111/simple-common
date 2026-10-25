package com.simple.common.auth.client.common.entity.login;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * 登录保存的用户临时信息
 * 用于存储用户登录后的会话信息
 * <p>
 * Jackson 反序列化兼容：显式 null 的 scopes/loginRole 不再抛 NPE（空值保持为 null，
 * 由消费方按无角色/无范围处理）。
 * </p>
 *
 * @author qty
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
@Schema(description = "登录保存的用户信息")
public class UserTemporary {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 登录标识(用于区分同一用户不同登录设备)
     */
    private String loginKey;

    /**
     * JWT唯一标识
     */
    private String jti;

    /**
     * 请求路径
     */
    private String path;

    /**
     * 客户端ID（如：xiaoyue_client）
     */
    private String clientId;

    /**
     * 客户端名称
     */
    private String clientName;

    /**
     * 客户端能访问的资源名称集合（微服务名称），多个用逗号分隔
     */
    private String appNames;

    /**
     * 微信小程序AppID
     */
    private String wxAppId;

    /**
     * 权限作用域(all,write,read)
     */
    private HashSet<String> scopes;

    /**
     * 登录角色集合
     */
    private HashSet<String> loginRole;

    /**
     * 平台超级管理员显式标识
     * <p>
     * true 时由 @HasAuthority 切面直接放行全部权限校验。该标识只能由授权中心在登录时
     * 按"角色 roleKey=admin 且角色归属租户 tenantCode=platform"的业务定义置位，
     * 与 loginRole 中是否包含某个角色名解耦：租户自己的 admin 角色不代表平台超管，
     * 角色命名不参与豁免判定。
     * </p>
     */
    private boolean superAdmin;

    /**
     * 身份归属域（平台控制台登录置位，固定值=platform；null/空=租户域令牌）
     * <p>由授权中心登录时随内省数据写入，消费方按该字段结构化区分令牌域归属
     * （非租户码/角色键字面量特判），如 /auth/user-info 按域路由取登录账号。</p>
     */
    private String domain;

    /**
     * 审计归属标记（代管行事令牌透传；值形态 platform:{平台员工标识}）
     * <p>delegation 签行事令牌时随载荷置位（D2 同通道传递），行事期间业务操作按该标记落审计归属；
     * 普通令牌该字段为 null（不参与序列化），既有消费方零影响。</p>
     */
    private String auditMarker;

    /**
     * 行事域标识（代管行事令牌透传；非空=当前登录上下文为代管行事上下文）
     * <p>仅由校验链行事分支（本地验签+delegation 吊销态实时判定通过后）自载荷构造，
     * 供后续链路结构化识别行事上下文（如授权范围门对平台代管凭据的项目绑定已在签发侧校验四锁定，
     * 不适用按客户端注册 scope 的 containsAll 契约）；普通令牌恒 null。</p>
     */
    private String actingDomain;

    /**
     * 扩展信息
     */
    private Object extension;

    /**
     * 数据权限对象
     */
    private DataPermission dataPermission;

    /**
     * null 安全：Jackson 显式反序列化 null 时不抛 NPE，Lombok 生成 setter 参数类型与字段对齐
     */
    public void setScopes(Collection<String> scopes) {
        this.scopes = scopes == null ? null : new HashSet<>(scopes);
    }

    /**
     * null 安全：Jackson 显式反序列化 null 时不抛 NPE
     */
    public void setLoginRole(Collection<String> loginRole) {
        this.loginRole = loginRole == null ? null : new HashSet<>(loginRole);
    }

    /**
     * @deprecated openId（微信 openid）与 loginKey（登录会话标识）语义不同，
     * 序列化导出 loginKey 属于凭证材料暴露；保留 getter 仅为源码兼容，不再参与序列化
     */
    @Deprecated
    @JsonIgnore
    public String getOpenId() {
        return loginKey;
    }
}
