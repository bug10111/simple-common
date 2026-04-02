package com.simple.oauth.common.dto.sysClientDetails;

import com.baomidou.mybatisplus.annotation.TableField;
import com.simple.oauth.common.enums.ServerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@Schema(title = "客户端信息(sys_client_details)创建请求参数")
public class CreateSysClientDetailsRequest {

    @Schema(description = "客户端名称")
    @NotEmpty(message = "客户端名称不能为空")
    private String clientName;

    @Schema(description = "服务")
    @NotEmpty(message = "服务不能为空")
    private String server;

    @Schema(description = "服务类型")
    @NotNull(message = "服务类型不能为空")
    private ServerType serverType;

    @Schema(description = "客户端（如：xiaoyue_client）")
    private String clientId;

    @Schema(description = "客户端密码（要加密后存储)，即秘钥")
    private String clientSecret;

    @Schema(description = "作用域")
    private List<String> scope;

    @Schema(description = "32位密钥字符串")
    private String hsKey;

    @Schema(description = "RSA公钥")
    private String rsaPublic;

    @Schema(description = "RSA私钥")
    private String rsaPrivate;

    @Schema(description = "（可选）token有效时间（单位秒），不填默认(60 * 60 * 12, 12小时)")
    private Integer accessTokenValidity;

    @Schema(description = "（可选）刷新令牌的有效时间（单位秒），不填默认(60 * 60 * 24 * 30, 30天)")
    private Integer refreshTokenValidity;

    @Schema(description = "是否有微信小程序")
    private Boolean hasWx;

    @Schema(description = "appid")
    private String wxAppId;

    @Schema(description = "wxAppSecret")
    private String wxAppSecret;

    @Schema(description = "备注")
    private String remark;

}

