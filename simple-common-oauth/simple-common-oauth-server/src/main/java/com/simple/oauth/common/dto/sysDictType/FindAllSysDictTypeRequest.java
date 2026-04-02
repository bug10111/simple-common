package com.simple.oauth.common.dto.sysDictType;

import com.simple.common.mp.page.PageBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "字典类型(sys_dict_type)列表请求参数")
public class FindAllSysDictTypeRequest extends PageBase {

    @Schema(description = "字典名称")
    private String dictName;

    @Schema(description = "字典类型")
    private String dictType;
}

