package com.simple.oauth.common.dto.sysAnnex;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "附件(sys_annex)修改请求参数")
public class UpdateSysAnnexRequest extends CreateSysAnnexRequest {

    @Schema(description = "主键")
    @NotEmpty(message = "主键不能为空")
    private String id;

}

