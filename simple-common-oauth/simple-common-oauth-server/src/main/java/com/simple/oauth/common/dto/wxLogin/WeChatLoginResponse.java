package com.simple.oauth.common.dto.wxLogin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
@Schema(description = "微信登录返回实体")
public class WeChatLoginResponse {

    @JsonProperty("session_key")
    private String sessionKey;

    @JsonProperty("unionid")
    private String unionId;

    @JsonProperty("openid")
    private String openid;

    @JsonProperty("errcode")
    private int errcode;

    @JsonProperty("errmsg")
    private String errmsg;
}
