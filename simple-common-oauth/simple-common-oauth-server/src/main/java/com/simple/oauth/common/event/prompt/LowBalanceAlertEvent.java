package com.simple.oauth.common.event.prompt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.eventbus.common.annotation.Event;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * Created with IntelliJ IDEA
 * Description: 设备低余额提示
 *
 * @author qty
 */
@Data
@Event(targets = "oauth")
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class LowBalanceAlertEvent {

    //地址
    private String parkSourceSelectionStr;

    //商户ID
    private String merchantId;

    //手机号
    private String phone;

    //设备类型
    private String deviceType;

    //设备号
    private String num;

    //水电余额阈值
    private BigDecimal sum;

}
