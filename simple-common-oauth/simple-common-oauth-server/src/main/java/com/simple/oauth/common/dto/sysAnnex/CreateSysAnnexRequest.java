package com.simple.oauth.common.dto.sysAnnex;

import com.simple.common.annex.common.enums.Algorithm;
import com.simple.common.annex.common.enums.ShareType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "附件(sys_annex)创建请求参数")
public class CreateSysAnnexRequest {

    @Schema(description = "名称")
    @NotEmpty(message = "名称不能为空")
    private String name;

    @Schema(description = "文件总大小")
    @NotNull(message = "文件总大小不能为空")
    private Long totalSize;

    @Schema(description = "摘要算法值")
    @NotEmpty(message = "摘要算法值不能为空")
    private String algorithmValue;

    @Schema(description = "摘要算法类型")
    @NotEmpty(message = "摘要算法类型不能为空")
    private Algorithm algorithmType;

    @Schema(description = "文件扩展名（不带.）")
    @NotEmpty(message = "文件扩展名（不带.）不能为空")
    private String suffix;

    @Schema(description = "文件在完整url")
    @NotEmpty(message = "文件在完整url不能为空")
    private String saveUrl;

    @Schema(description = "附件类型")
    @NotEmpty(message = "附件类型不能为空")
    private ShareType shareType;

    @Schema(description = "系统名称")
    @NotEmpty(message = "系统名称不能为空")
    private String applicationName;
}

