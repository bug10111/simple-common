package com.simple.common.xxljob.common.enums;

import cn.hutool.core.util.StrUtil;
import com.simple.common.xxljob.config.XxlJobConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 * Description: xxl-job远程url
 *
 * @author qty
 */
@Getter
@AllArgsConstructor
public enum XxlJobRequestUrl {

    ADD("/jobinfo/add", "添加"),
    UPDATE("/jobinfo/update", "修改"),
    DELETE("/jobinfo/remove", "删除"),
    START("/jobinfo/start", "启动"),
    END("/jobinfo/stop","停止"),
    TRIGGER("/jobinfo/trigger", "执行"),

    ;

    private final String url;

    private final String desc;

    /**
     * 获取请求路径
     * <p>
     * 对 adminAddresses 做快速失败与规范化：未配置/为空白时直接抛出可诊断异常，
     * 而非拼出 {@code null/jobinfo/add} 之类在 HTTP 层才失败的谜之地址；
     * 剔除结尾多余斜杠；多地址写法（逗号分隔）属于执行器注册语义，本管理通道仅支持单地址，
     * 误配置时快速失败而非产出 {@code http://a,http://b/jobinfo/add} 的坏 URL。
     *
     * @param xxlJobConfig 配置文件
     * @return 规范化后的完整请求地址
     */
    public String url(XxlJobConfig xxlJobConfig) {
        if (xxlJobConfig == null || StrUtil.isBlank(xxlJobConfig.getAdminAddresses())) {
            throw new IllegalStateException("xxl.job.admin-addresses 未配置，无法构造 xxl-job Admin 请求地址");
        }
        String adminAddresses = xxlJobConfig.getAdminAddresses().trim();
        if (adminAddresses.contains(",")) {
            throw new IllegalStateException("xxl.job.admin-addresses 管理通道仅支持单个地址: " + adminAddresses);
        }
        while (adminAddresses.endsWith("/")) {
            adminAddresses = adminAddresses.substring(0, adminAddresses.length() - 1);
        }
        return adminAddresses + this.getUrl();
    }
}
