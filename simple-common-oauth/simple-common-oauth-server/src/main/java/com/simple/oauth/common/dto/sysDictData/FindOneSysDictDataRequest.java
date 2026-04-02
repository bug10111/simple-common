package com.simple.oauth.common.dto.sysDictData;

import com.simple.common.mp.common.enums.DeleteState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
@Schema(title = "字典数据(sys_dict_data)列表请求参数")
public class FindOneSysDictDataRequest {

    @Schema(description = "字典编码")
    private String id;

    @Schema(description = "字典排序")
    private Integer serial;

    @Schema(description = "字典标签")
    private String dictLabel;

    @Schema(description = "字典键值")
    private String dictValue;

    @Schema(description = "字典类型")
    private String dictType;

    @Schema(description = "删除")
    private DeleteState deleted;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;

}

