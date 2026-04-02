package com.simple.oauth.common.entity.sms;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
@Schema(description = "低余额短信请求实体")
public class LowBalanceAlert {

    //地址
    private String address;

    //地址
    private String msg;

    //地址
    private BigDecimal sum;
}
