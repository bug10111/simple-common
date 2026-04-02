package com.simple.oauth.common.dto.sysAnnouncement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "系统公告(sys_announcement)修改请求参数")
public class UpdateSysAnnouncementRequest extends CreateSysAnnouncementRequest {

    @Schema(description = "主键")
    @NotEmpty(message = "主键不能为空")
    private String id;

}

