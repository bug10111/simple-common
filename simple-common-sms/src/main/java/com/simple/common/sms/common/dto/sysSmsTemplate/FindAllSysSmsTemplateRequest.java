package com.simple.common.sms.common.dto.sysSmsTemplate;

import com.simple.common.mp.common.enums.DeleteState;
import com.simple.common.mp.page.PageBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(title = "短信模板(sys_sms_template)列表请求参数")
public class FindAllSysSmsTemplateRequest extends PageBase {

    @Schema(description = "短信签名")
    private String signName;

    @Schema(description = "模板code")
    private String templateCode;

    @Schema(description = "数据状态")
    private DeleteState deleted;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "修改时间")
    private Date updateTime;

}

