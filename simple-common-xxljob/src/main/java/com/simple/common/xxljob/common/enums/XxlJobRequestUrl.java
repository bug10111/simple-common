package com.simple.common.xxljob.common.enums;

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

    private final String label;

    /**
     * 获取请求路径
     *
     * @param xxlJobConfig 配置文件
     */
    public String url(XxlJobConfig xxlJobConfig) {
        return xxlJobConfig.getAdminAddresses() + this.getUrl();
    }
}
