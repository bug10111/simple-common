package com.simple.oauth.common.dto.sysDictData;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "字典数据(sys_dict_data)修改请求参数")
public class UpdateSysDictDataRequest {

    @Schema(description = "字典编码")
    @NotEmpty(message = "字典编码不能为空")
    private String id;

    @Schema(description = "字典标签")
    @NotEmpty(message = "字典标签不能为空")
    private String dictLabel;

    @Schema(description = "字典类型")
    @NotEmpty(message = "字典类型不能为空")
    private String dictType;

    @Schema(description = "字典类型")
    @NotEmpty(message = "字典类型不能为空")
    private String dictValue;

    @Schema(description = "字典排序")
    @NotNull(message = "字典排序不能为空")
    private Integer serial;

    @Schema(description = "备注")
    private String remark;

}

