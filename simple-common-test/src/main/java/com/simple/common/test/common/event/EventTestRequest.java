package com.simple.common.test.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.simple.common.eventbus.common.annotation.Event;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Data
@Event
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class EventTestRequest {

    @NotEmpty(message = "姓名不能为空")
    @Schema(description = "名称")
    private String name;

    @NotEmpty(message = "性别不能为空")
    @Schema(description = "性别")
    private String sex;

}
