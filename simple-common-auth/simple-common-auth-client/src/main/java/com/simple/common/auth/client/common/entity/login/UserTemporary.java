package com.simple.common.auth.client.common.entity.login;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
@Schema(description = "登录保存的用户信息")
public class UserTemporary {

    //用户id
    private String userId;

    //用户名称
    private String nickname;

    //loginKey
    private String loginKey;

    //jti
    private String jti;

    //请求路径
    private String path;

    //客户端（如：xiaoyue_client）
    private String clientId;

    //客户端名称
    private String clientName;

    //预留字段，客户端能访问的资源名称集合（微服务名称），多个用逗号分隔
    private String appNames;

    //微信appid
    private String wxAppId;

    //作用域all,write,read
    private HashSet<String> scopes;

    //登录角色
    private HashSet<String> loginRole;

    //扩展信息
    private Object extension;

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
