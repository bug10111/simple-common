package com.simple.oauth.common.dto.sysAdvertisement;

import com.simple.common.mp.common.enums.Status;
import com.simple.common.mp.page.PageBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(title = "广告表(sys_advertisement)列表请求参数")
public class FindAllSysAdvertisementRequest extends PageBase {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "客户端id")
    private String clientId;

    @Schema(description = "类型（字典）")
    private String type;

    @Schema(description = "是否有外链")
    private String isLink;

    @Schema(description = "状态")
    private Status status;
}

