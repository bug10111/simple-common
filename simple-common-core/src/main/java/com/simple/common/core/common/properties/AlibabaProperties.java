package com.simple.common.core.common.properties;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 *  Description: 阿里云配置类
 * <p>说明：凭证采用"使用时快速失败"策略——消费方创建客户端前调用 {@link #validate()}；
 * 不做启动期强制校验，避免未使用短信能力的应用被迫配置阿里云凭证（设计取舍见回执）。</p>
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.ali")
public class AlibabaProperties {

    //访问键ID
    private String accessKeyId;

    //访问键密钥（toString已脱敏，防调试日志意外泄露）
    private String accessKeySecret;

    /**
     * 消费前校验：凭证缺失时给出明确错误，而不是到阿里云SDK处报含混的鉴权失败
     */
    public void validate() {
        if (StrUtil.isBlank(accessKeyId) || StrUtil.isBlank(accessKeySecret)) {
            throw new IllegalStateException("阿里云凭证未配置：simple.ali.access-key-id / simple.ali.access-key-secret 不能为空");
        }
    }

    @Override
    public String toString() {
        // 密钥不进日志；accessKeyId仅保留前4位便于核对
        return "AlibabaProperties{accessKeyId=" + StrUtil.subPre(accessKeyId, 4) + "***, accessKeySecret=***}";
    }
}
