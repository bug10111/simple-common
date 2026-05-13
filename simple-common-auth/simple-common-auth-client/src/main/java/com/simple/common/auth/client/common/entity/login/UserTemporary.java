package com.simple.common.auth.client.common.entity.login;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.List;

/**
 * 登录保存的用户临时信息
 * 用于存储用户登录后的会话信息
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
     * 扩展信息
     */
    private Object extension;

    /**
     * 数据权限对象
     */
    private DataPermission dataPermission;

    public void setScopes(List<String> scopes) {
        this.scopes = new HashSet<>(scopes);
    }

    @Deprecated
    public String getOpenId() {
        return loginKey;
    }

    public void setLoginRole(List<String> loginRole) {
        this.loginRole = new HashSet<>(loginRole);
    }
}