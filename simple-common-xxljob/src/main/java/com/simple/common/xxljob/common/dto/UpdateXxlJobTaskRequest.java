package com.simple.common.xxljob.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Data
@Accessors(chain = true)
@Schema(description = "修改任务请求类")
public class UpdateXxlJobTaskRequest extends CreateXxlJobTaskRequest {

    @Schema(description = "主键")
    private Integer id;

}
