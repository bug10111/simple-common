package com.simple.common.sms.common.dto.sysSmsTemplate;

import com.simple.common.mp.page.PageBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 短信模板(sys_sms_template)列表请求参数
 * <p>
 * 注意：{@code deleted} 为 @TableLogic 列,框架自动追加 deleted=0 条件,
 * 该字段不再作为查询条件暴露;时间字段不再提供精确等值过滤。
 * </p>
 *
 * @author qty
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(title = "短信模板(sys_sms_template)列表请求参数")
public class FindAllSysSmsTemplateRequest extends PageBase {

    @Schema(description = "短信签名")
    private String signName;

    @Schema(description = "模板code")
    private String templateCode;

    @Schema(description = "短信发送类型")
    private String sendType;

}
