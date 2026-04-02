package com.simple.oauth.common.dto.sysAdvertisement;

import java.util.Date;

import com.simple.common.mp.common.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
@Schema(title = "广告表(sys_advertisement)创建请求参数")
public class CreateSysAdvertisementRequest {

    @Schema(description = "名称")
    @NotEmpty(message = "名称不能为空")
    private String name;

    @Schema(description = "客户端id（不是主键）")
    @NotEmpty(message = "客户端id不能为空")
    private String clientId;

    @Schema(description = "类型（字典）")
    @NotEmpty(message = "类型（字典）不能为空")
    private String type;

    @Schema(description = "图片")
    @NotEmpty(message = "图片不能为空")
    private String image;

    @Schema(description = "是否有外链")
    @NotEmpty(message = "是否有外链不能为空")
    private String isLink;

    @Schema(description = "外链地址")
    private String link;

    @Schema(description = "排序")
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "开始时间")
    @NotNull(message = "开始时间不能为空")
    private Date beginTime;

    @Schema(description = "结束时间")
    @NotNull(message = "结束时间不能为空")
    private Date endTime;
}

