package com.simple.oauth.common.dto.wxLogin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Data
@Accessors(chain = true)
@Schema(description = "微信登录需要的信息")
public class WeChatLoginRequest {

    @NotBlank(message = "code不能为空")
    @Schema(title = "wx.login后返回的code")
    private String code;

    @Schema(title = "wx.login后返回的加密数据")
    private String encryptedData;

    @Schema(title = "wx.login后返回的iv")
    private String iv;

}
