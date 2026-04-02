package com.simple.oauth.common.dto.sysAnnouncement;

import com.simple.common.mp.common.enums.Status;
import com.simple.common.mp.page.PageBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(title = "系统公告(sys_announcement)列表请求参数")
public class FindAllSysAnnouncementRequest extends PageBase {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "状态")
    private Status status;
}

