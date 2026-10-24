package com.simple.common.auth.server.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.simple.common.auth.client.common.entity.login.DataPermission;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.Collections;
import java.util.Set;

/**
 * Created with IntelliJ IDEA
 * <p>账户状态标志（是否过期/锁定/凭证过期/可用）缺省为 0（fail-closed）：
 * 登录实现方必须显式设置四个标志，未设置的账户一律拒绝登录，防止缺省放行。</p>
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
    @JsonIgnore
    private Object extension;

    //扩展信息,登录后直接返回给前端
    private Map<String, String> extensionResponse;

    //数据权限（用户所有角色合并后的数据权限）
    private DataPermission dataPermission;

    //平台超级管理员显式标识（授权中心登录时按业务定义置位，随载荷进入 login:info 缓存；null=未置位，按非超管处理）
    private Boolean superAdmin;

    //帐户是否过期：1-未过期，0-已过期
    @JsonIgnore
    private int isAccountNonExpired = 0;

    //帐户是否被锁定：1-未锁定，0-已锁定
    @JsonIgnore
    private int isAccountNonLocked = 0;

    //密码是否过期：1-未过期，0-已过期
    @JsonIgnore
    private int isCredentialsNonExpired = 0;

    //帐户是否可用：1-可用，0-删除用户
    @JsonIgnore
    private int isEnabled = 0;

    /**
     * 获取登录角色（不可变视图，防止认证通过后被外部篡改）
     */
    public Set<String> getLoginRole() {
        return loginRole == null ? null : java.util.Collections.unmodifiableSet(loginRole);
    }

}