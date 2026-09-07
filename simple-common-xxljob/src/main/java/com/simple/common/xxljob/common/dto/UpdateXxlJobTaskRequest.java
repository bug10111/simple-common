package com.simple.common.xxljob.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
@Schema(description = "修改任务请求类")
public class UpdateXxlJobTaskRequest extends CreateXxlJobTaskRequest {

    @Schema(description = "主键")
    @NotNull(message = "任务主键不能为空")
    private Integer id;

}
