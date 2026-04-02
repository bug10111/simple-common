package com.simple.oauth.common.dto.sysClientDetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.mp.common.enums.Status;
import com.simple.oauth.common.enums.ServerType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
@Schema(title = "客户端信息(sys_client_details)明细响应")
public class SysClientDetailsPageResponse {

    @Schema(description = "客户端信息id")
    private String id;

    @Schema(description = "服务")
    private String server;

    @Schema(description = "服务类型")
    private ServerType serverType;

    @Schema(description = "客户端名称")
    private String clientName;

    @Schema(description = "客户端（如：xiaoyue_client）")
    private String clientId;

    @Schema(description = "预留字段，客户端能访问的资源id集合（微服务名称），多个用逗号分隔")
    private String resourceIds;

    @Schema(description = "作用域all,write,read")
    private List<String> scope;

    @Schema(description = "（可选）token有效时间（单位秒），不填默认(60 * 60 * 12, 12小时)")
    private Integer accessTokenValidity;

    @Schema(description = "（可选）刷新令牌的有效时间（单位秒），不填默认(60 * 60 * 24 * 30, 30天)")
    private Integer refreshTokenValidity;

    @Schema(description = "32位密钥字符串")
    private String hsKey;

    @Schema(description = "RSA公钥")
    private String rsaPublic;

    @Schema(description = "是否有微信小程序")
    private Boolean hasWx;

    @Schema(description = "appid")
    private String wxAppId;

    @Schema(description = "预留字段，若填写建议为map（字典code，值）的json")
    private Map<String, Object> reserve;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态")
    private Status status;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "修改时间")
    private Date updateTime;
}

