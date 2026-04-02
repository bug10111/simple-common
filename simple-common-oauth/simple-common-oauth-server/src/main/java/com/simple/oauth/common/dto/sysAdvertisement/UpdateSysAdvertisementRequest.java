package com.simple.oauth.common.dto.sysAdvertisement;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(title = "广告表(sys_advertisement)修改请求参数")
public class UpdateSysAdvertisementRequest extends CreateSysAdvertisementRequest {

    @Schema(description = "主键")
    @NotEmpty(message = "主键不能为空")
    private String id;

}

