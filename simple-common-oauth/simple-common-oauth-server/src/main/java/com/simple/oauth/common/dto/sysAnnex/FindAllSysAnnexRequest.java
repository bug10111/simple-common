package com.simple.oauth.common.dto.sysAnnex;

import com.simple.common.annex.common.enums.ShareType;
import com.simple.common.mp.page.PageBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "附件(sys_annex)列表请求参数")
public class FindAllSysAnnexRequest extends PageBase {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "摘要算法值")
    private String algorithmValue;

    @Schema(description = "文件在完整url")
    private String saveUrl;

    @Schema(description = "附件类型")
    private ShareType shareType;

    @Schema(description = "系统名称")
    private String applicationName;
}

