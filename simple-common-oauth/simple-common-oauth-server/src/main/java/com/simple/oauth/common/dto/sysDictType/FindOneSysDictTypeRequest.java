package com.simple.oauth.common.dto.sysDictType;

import com.simple.common.mp.common.enums.DeleteState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
@Schema(title = "字典类型(sys_dict_type)列表请求参数")
public class FindOneSysDictTypeRequest {

    @Schema(description = "字典主键")
    private String id;

    @Schema(description = "字典名称")
    private String dictName;

    @Schema(description = "字典类型")
    private String dictType;

    @Schema(description = "删除：1-已删除，0-未删除")
    private DeleteState deleted;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;

}

