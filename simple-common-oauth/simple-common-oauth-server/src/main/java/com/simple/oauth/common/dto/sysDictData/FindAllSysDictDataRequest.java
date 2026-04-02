package com.simple.oauth.common.dto.sysDictData;

import com.simple.common.mp.page.PageBase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "字典数据(sys_dict_data)列表请求参数")
public class FindAllSysDictDataRequest extends PageBase {

    @Schema(description = "字典标签")
    private String dictLabel;

    @Schema(description = "字典键值")
    private String dictValue;

    @Schema(description = "字典类型")
    @NotEmpty(message = "字典类型不能为空")
    private String dictType;

}

