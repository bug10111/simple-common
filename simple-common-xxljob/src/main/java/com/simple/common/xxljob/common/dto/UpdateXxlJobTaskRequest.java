package com.simple.common.xxljob.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 * <p>
 * 更新请求与创建请求共用字段集，数值字段在父类已声明为可空 Integer：
 * 未显式赋值的字段不会进入 Admin 表单（BeanUtils.toMap 剔除 null），
 * 调用方漏填超时/重试次数不会把既有任务的配置静默重置。
 * 更新语义为「所传字段全量覆盖」：需要保留的字段请显式回填原值。
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "修改任务请求类")
public class UpdateXxlJobTaskRequest extends CreateXxlJobTaskRequest {

    @Schema(description = "主键")
    @NotNull(message = "任务主键不能为空")
    @Positive(message = "任务主键必须为正数")
    private Integer id;

}
