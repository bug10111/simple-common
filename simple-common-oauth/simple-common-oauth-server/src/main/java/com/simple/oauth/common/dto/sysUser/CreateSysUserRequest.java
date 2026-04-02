package com.simple.oauth.common.dto.sysUser;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@Schema(title = "用户(sys_user)创建请求参数")
public class CreateSysUserRequest {

    @Schema(description = "用户账号")
    @NotEmpty(message = "用户账号不能为空")
    private String username;

    @Schema(description = "用户名称")
    private String nickname;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "旧密码")
    private String passwordOld;

    @Schema(description = "新密码，修改密码时候，旧密码必填")
    private String passwordNew;

    @Schema(description = "角色列表")
    List<BindingRoleRequest> roleIds;

    @Schema(description = "帐户是否过期：1-未过期，0-已过期")
    private Integer isAccountNonExpired = 1;

    @Schema(description = "帐户是否被锁定：1-未锁定，0-已锁定")
    private Integer isAccountNonLocked = 1;

    @Schema(description = "密码是否过期：1-未过期，0-已过期")
    private Integer isCredentialsNonExpired = 1;

    @Schema(description = "帐户是否可用：1-可用，0-禁用")
    private Integer isEnabled = 1;

    @Schema(description = "扩展")
    private Map<String, Object> reserve;
}

