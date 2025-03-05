package com.simple.common.auth.server.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Created with IntelliJ IDEA
 * 需要构建的客户端信息对象
 *
 * @author 兄台丶请冷静
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
public class ClientDetails {

    //客户端（如：xiaoyue_client）
    private String clientId;

    //客户端名称
    private String clientName;

    //客户端密码（要加密后存储)，即秘钥
    private String clientSecret;

    //微信appid
    private String wxAppId;

    //预留字段，客户端能访问的资源名称集合（微服务名称），多个用逗号分隔
    private String resourceIds;

    //作用域all,write,read
    private List<String> scope;

    //（可选）token有效时间（单位秒），不填默认(60 * 60 * 12, 12小时)
    private int accessTokenValidity;

    //（可选）刷新令牌的有效时间（单位秒），不填默认(60 * 60 * 24, 30天)
    private int refreshTokenValidity;

}
