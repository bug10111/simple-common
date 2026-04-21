package com.simple.oauth.common.dto.login;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
@Schema(description = "刷新登录请求")
public class RefreshDto {

    @Schema(description = "刷新token")
    private String refresh;

}
