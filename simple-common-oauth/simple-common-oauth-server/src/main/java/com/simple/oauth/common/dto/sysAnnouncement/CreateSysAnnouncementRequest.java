package com.simple.oauth.common.dto.sysAnnouncement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
@Schema(title = "系统公告(sys_announcement)创建请求参数")
public class CreateSysAnnouncementRequest {

    @Schema(description = "标题")
    @NotEmpty(message = "标题不能为空")
    private String title;

    @Schema(description = "数据")
    @NotEmpty(message = "数据不能为空")
    private String data;

    @Schema(description = "开始时间")
    @NotNull(message = "开始时间不能为空")
    private Date beginTime;

    @Schema(description = "结束时间")
    @NotNull(message = "结束时间不能为空")
    private Date endTime;
}

