package com.simple.oauth.common.dto.sysUserLoginKey;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
@Schema(title = "用户登录标志(sys_user_login_key)列表请求参数")
public class FindOneSysUserLoginKeyRequest {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "用户Id")
    private String userId;

    @Schema(description = "第三方登录标志")
    private String loginKey;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "修改时间")
    private Date updateTime;

}

