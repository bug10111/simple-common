package com.simple.oauth.common.dto.sysDictType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "字典类型(sys_dict_type)修改请求参数")
public class UpdateSysDictTypeRequest {

    @Schema(description = "字典主键")
    @NotEmpty(message = "字典主键不能为空")
    private String id;

    @Schema(description = "字典名称")
    @NotEmpty(message = "字典名称不能为空")
    private String dictName;

    @Schema(description = "备注")
    private String remark;

}

