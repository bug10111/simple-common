package com.simple.common.annex.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.simple.common.annex.common.enums.Algorithm;
import com.simple.common.annex.common.enums.ShareType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
@Schema(description = "附件上传的返回参数")
public class UploadResponse {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "文件总大小(单位：字节);未携带判断回调时该值始终有值,直接使用回调返回的场景可能为 null")
    private Long totalSize;

    @Schema(description = "摘要算法值：用于文件去重;仅当配置的摘要算法生效时才有值,使用前必须判空")
    private String algorithmValue;

    @Schema(description = "摘要算法类型")
    private Algorithm algorithmType;

    @Schema(description = "文件扩展名(不带.)")
    private String suffix;

    @Schema(description = "文件完整访问 URL;shareType 为 PRIVATE 时该值仅作为后续 generateUrl/delete 的入参标识,不应作为可访问链接直接暴露给终端用户")
    private String saveUrl;

    @Schema(description = "附件类型")
    private ShareType shareType;

    @Schema(description = "系统名称")
    private String applicationName;

    /**
     * 命名说明：字段携带 is 前缀是既有契约,JSON 属性名固定为 {@code isTrue};
     * 显式声明 @JsonProperty 锁定序列化名,避免包装类型改为基本类型后 getter 命名变化导致 JSON 字段意外改名。
     * 语义：仅当配置了去重回调且回调返回 true(文件已存在,跳过上传)时为 true;完成真实上传后为 false。
     */
    @Schema(description = "携带去重回调时返回:true 表示文件已存在并跳过上传,false 表示完成了真实上传")
    @JsonProperty("isTrue")
    private Boolean isTrue;

    @Schema(description = "预留的业务扩展字段(调用方自行填充),框架不写入该字段;与 suffix(从文件名解析的后缀)语义不同,注意区分")
    private String extension;
}
