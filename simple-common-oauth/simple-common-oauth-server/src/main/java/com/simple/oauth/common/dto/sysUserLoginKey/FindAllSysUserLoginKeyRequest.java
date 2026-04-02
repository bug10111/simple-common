package com.simple.oauth.common.dto.sysUserLoginKey;

import com.simple.common.mp.page.PageBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "用户登录标志(sys_user_login_key)列表请求参数")
public class FindAllSysUserLoginKeyRequest extends PageBase {

    @Schema(description = "用户Id")
    private String userId;

    @Schema(description = "第三方登录标志")
    private String loginKey;
}

