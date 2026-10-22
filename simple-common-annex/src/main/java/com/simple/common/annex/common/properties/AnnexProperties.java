package com.simple.common.annex.common.properties;

import com.simple.common.annex.common.enums.Algorithm;
import com.simple.common.annex.common.enums.AnnexType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Created with IntelliJ IDEA
 * Description: minio配置类
 * <p>
 * 必填项缺失或取值非法时在启动装配阶段即校验失败(fail-fast),而不是等到首次上传时抛出晦涩的 SDK 异常。
 * </p>
 *
 * @author qty
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "simple.annex")
public class AnnexProperties {

    //文件存储类型(常量名为外部配置契约,禁止改名)
    private AnnexType type = AnnexType.MINIO;

    //服务地址(必填)
    @NotBlank(message = "simple.annex.server-url 不能为空")
    private String serverUrl;

    //预签名 URL 有效时长(单位：分钟,必须为正,防止生成即过期的签名 URL)
    @NotNull(message = "simple.annex.expire-time 不能为空")
    @Min(value = 1, message = "simple.annex.expire-time 必须为正数(单位：分钟)")
    private Integer expireTime = 30;

    //minio key(必填)
    @NotBlank(message = "simple.annex.access-key 不能为空")
    private String accessKey;

    //minio secret(必填;排除出 toString,防止凭据经日志/actuator 泄漏)
    @NotBlank(message = "simple.annex.access-secret 不能为空")
    @ToString.Exclude
    private String accessSecret;

    //摘要算法,用于判断文件是否重复上传;默认 SHA-256(抗碰撞性优于 MD5,更符合去重场景)
    private Algorithm algorithm = Algorithm.SHA_256;

    //区域，默认us-east-1，其余区域参考S3 Region，阿里云的默认cn-chengdu
    private String region = "us-east-1";
}
