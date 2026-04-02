package com.simple.oauth.common.dto.sysDictType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "字典类型(sys_dict_type)创建请求参数")
public class CreateSysDictTypeRequest {

    @Schema(description = "字典名称")
    @NotEmpty(message = "字典名称不能为空")
    private String dictName;

    @Schema(description = "字典类型")
    @NotEmpty(message = "字典类型不能为空")
    private String dictType;

    @Schema(description = "备注")
    private String remark;
}

