package com.simple.oauth.common.dto.sysUser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
@Schema(title = "用户(sys_user)明细响应")
public class SysUserInfoResponse {

    @Schema(description = "用户id")
    private String id;

    @Schema(description = "用户账号")
    private String username;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "帐户是否过期：1-未过期，0-已过期")
    private Integer isAccountNonExpired;

    @Schema(description = "帐户是否被锁定：1-未锁定，0-已锁定")
    private Integer isAccountNonLocked;

    @Schema(description = "密码是否过期：1-未过期，0-已过期")
    private Integer isCredentialsNonExpired;

    @Schema(description = "帐户是否可用：1-可用，0-禁用")
    private Integer isEnabled;

    @Schema(description = "扩展")
    private Map<String, Object> reserve;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;
}

