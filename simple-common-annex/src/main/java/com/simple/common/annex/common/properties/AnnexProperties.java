package com.simple.common.annex.common.properties;

import com.simple.common.annex.common.enums.Algorithm;
import com.simple.common.annex.common.enums.AnnexType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: minio配置类
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.annex")
public class AnnexProperties {

    //文件存储类型
    private AnnexType type = AnnexType.MINIO;

    //服务地址
    private String serverUrl;

    //有效时长，单位分钟
    private Integer expireTime = 30;

    //minio key
    private String accessKey;

    //minio Secret
    private String accessSecret;

    //算法，用于判断文件是否重复上传
    private Algorithm algorithm = Algorithm.MD5;

    //区域，默认us-east-1，其余区域参考S3 Region，阿里云的默认cn-chengdu
    private String region = "us-east-1";
}
