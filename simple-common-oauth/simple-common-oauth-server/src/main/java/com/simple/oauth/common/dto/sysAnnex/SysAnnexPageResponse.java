package com.simple.oauth.common.dto.sysAnnex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.annex.common.enums.Algorithm;
import com.simple.common.annex.common.enums.ShareType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
@Schema(title = "附件(sys_annex)明细响应")
public class SysAnnexPageResponse {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "文件总大小")
    private Long totalSize;

    @Schema(description = "摘要算法值")
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

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "修改时间")
    private Date updateTime;
}

