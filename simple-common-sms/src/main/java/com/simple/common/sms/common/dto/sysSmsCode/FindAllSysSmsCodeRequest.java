package com.simple.common.sms.common.dto.sysSmsCode;

import com.simple.common.mp.common.enums.DeleteState;
import com.simple.common.mp.common.enums.Status;
import com.simple.common.mp.page.PageBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(title = "短信验证码(sys_code_record)列表请求参数")
public class FindAllSysSmsCodeRequest extends PageBase {

    @Schema(description = "短信类型")
    private String sendType;

    @Schema(description = "日期")
    private String date;

    @Schema(description = "电话号码")
    private String phone;

    @Schema(description = "验证码")
    private String code;

    @Schema(description = "IP地址")
    private String ip;

    @Schema(description = "请求状态")
    private Status reqStatus;

    @Schema(description = "请求结果")
    private String reqResults;

    @Schema(description = "使用状态")
    private Status status;

    @Schema(description = "删除")
    private DeleteState deleted;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "修改时间")
    private Date updateTime;

}

