package com.simple.common.sms.common.dto.sysSmsCode;

import com.simple.common.mp.common.enums.Status;
import com.simple.common.mp.page.PageBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.Size;

/**
 * 短信验证码(sys_sms_code)列表请求参数
 * <p>
 * 安全约束：本查询对象可用于按手机号、验证码检索记录(验证码字段经 WRITE_ONLY 序列化不会回显),
 * 如需绑定 HTTP 查询端点,必须自行叠加授权、限流与响应脱敏;当前仅限服务端内部流程使用。
 * </p>
 * <p>注意：{@code deleted} 为 @TableLogic 列,框架自动追加 deleted=0 条件,
 * 该字段不再作为查询条件暴露;时间字段只支持范围语义,不再提供精确等值过滤。</p>
 *
 * @author qty
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(title = "短信验证码(sys_sms_code)列表请求参数")
public class FindAllSysSmsCodeRequest extends PageBase {

    @Schema(description = "短信类型")
    private String sendType;

    @Schema(description = "日期")
    private String date;

    @Size(max = 32, message = "电话号码长度不能超过32")
    @Schema(description = "电话号码")
    private String phone;

    @Size(max = 16, message = "验证码长度不能超过16")
    @Schema(description = "验证码")
    private String code;

    @Size(max = 64, message = "IP地址长度不能超过64")
    @Schema(description = "IP地址")
    private String ip;

    @Schema(description = "请求状态")
    private Status reqStatus;

    @Schema(description = "请求结果")
    private String reqResults;

    @Schema(description = "使用状态")
    private Status status;

}
