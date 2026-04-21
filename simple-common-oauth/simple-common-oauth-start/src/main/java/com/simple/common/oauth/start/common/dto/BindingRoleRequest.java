package com.simple.common.oauth.start.common.dto;

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
@Schema(description = "用户绑定角色请求类")
public class BindingRoleRequest {

    @Schema(description = "角色ID")
    @NotEmpty(message = "角色ID不能为空")
    private String roleId;
}
