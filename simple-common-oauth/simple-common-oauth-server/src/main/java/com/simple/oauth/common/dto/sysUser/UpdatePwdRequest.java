package com.simple.oauth.common.dto.sysUser;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
@Schema(description = "修改密码请求类")
public class UpdatePwdRequest {

    @Schema(description = "用户ID")
    private String id;

    @Schema(description = "旧密码")
    @NotEmpty(message = "旧密码不能为空")
    private String oldPwd;

    @Schema(description = "新密码")
    @NotEmpty(message = "新密码不能为空")
    private String newPwd;
}
