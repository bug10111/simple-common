package com.simple.oauth.common.dto.wxLogin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Data
@Accessors(chain = true)
@Schema(description = "微信登录需要收集的数据对象")
public class WechatInfoDTO {

    private String unionId;

    private String openId;

    private String userPhone;
}
