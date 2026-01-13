package com.simple.common.auth.server.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA
 * 用户登录需要构建的基类数据
 *
 * @author qty
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
public class AbsUserDetails {

    //用户id
    private String userId;

    //用户名称
    private String nickname;

    //角色
    private Set<String> loginRole;

    //loginKey 第三方登录的身份标志
    private String loginKey;

    //扩展信息,登录后服务端可以全局获取到，后续会自动转化json
    private Object extension;

    //扩展信息,登录后直接返回给前端
    private Map<String, String> extensionResponse;

    //帐户是否过期：1-未过期，0-已过期
    @JsonIgnore
    private int isAccountNonExpired = 1;

    //帐户是否被锁定：1-未锁定，0-已锁定
    @JsonIgnore
    private int isAccountNonLocked = 1;

    //密码是否过期：1-未过期，0-已过期
    @JsonIgnore
    private int isCredentialsNonExpired = 1;

    //帐户是否可用：1-可用，0-删除用户
    @JsonIgnore
    private int isEnabled = 1;
}
