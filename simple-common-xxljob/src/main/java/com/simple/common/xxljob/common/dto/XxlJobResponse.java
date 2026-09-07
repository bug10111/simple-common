package com.simple.common.xxljob.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 * Description: xxl-job Admin统一返回对象
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
@Schema(description = "xxl-job Admin统一返回对象")
public class XxlJobResponse {

    @Schema(description = "返回码，200表示成功")
    private Integer code;

    @Schema(description = "返回提示信息")
    private String msg;

    @Schema(description = "返回数据")
    private String content;

}
