package com.simple.common.annex.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.simple.common.annex.common.enums.Algorithm;
import com.simple.common.annex.common.enums.ShareType;
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
@Schema(description = "附件上传的返回参数")
public class UploadResponse {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "文件总大小")
    private long totalSize;

    @Schema(description = "摘要算法值：验证文件一致性")
    private String algorithmValue;

    @Schema(description = "摘要算法类型")
    private Algorithm algorithmType;

    @Schema(description = "文件扩展名（不带.）")
    private String suffix;

    @Schema(description = "文件在完整url")
    private String saveUrl;

    @Schema(description = "附件类型")
    private ShareType shareType;

    @Schema(description = "系统名称")
    private String applicationName;

    @Schema(description = "有判断方法的时候返回,表示是否存在文件")
    private Boolean isTrue;

    @Schema(description = "有判断方法的时候返回,扩展")
    private String extension;
}
