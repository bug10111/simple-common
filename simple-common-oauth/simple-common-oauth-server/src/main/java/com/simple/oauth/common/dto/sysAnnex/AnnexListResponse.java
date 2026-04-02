package com.simple.oauth.common.dto.sysAnnex;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Data
@Accessors(chain = true)
@Schema(description = "获取文件集合")
public class AnnexListResponse {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "文件在完整url")
    private String saveUrl;

    @Schema(description = "文件名称")
    private String name;
}
