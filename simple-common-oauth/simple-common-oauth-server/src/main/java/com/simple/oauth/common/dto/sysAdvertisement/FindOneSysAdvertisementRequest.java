package com.simple.oauth.common.dto.sysAdvertisement;

import java.util.Date;

import com.simple.common.mp.page.PageBase;
import io.swagger.v3.oas.annotations.media.Schema;
import com.simple.common.mp.common.enums.DeleteState;
import com.simple.common.mp.common.enums.Status;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "广告表(sys_advertisement)列表请求参数")
public class FindOneSysAdvertisementRequest {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "客户端id")
    private String clientId;

    @Schema(description = "类型（字典）")
    private String type;

    @Schema(description = "图片")
    private String image;

    @Schema(description = "是否有外链")
    private String isLink;

    @Schema(description = "外链地址")
    private String link;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "开始时间")
    private Date beginTime;

    @Schema(description = "结束时间")
    private Date endTime;

    @Schema(description = "状态")
    private Status status;

}

