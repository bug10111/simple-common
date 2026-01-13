package com.simple.common.logs.client.common.event;

import cn.hutool.core.date.DateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.eventbus.common.annotation.Event;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 * Description: 日志事件
 *
 * @author qty
 */
@Data
@Event(targets = "common-test")
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class LogDataEvent {

    /**
     * 方法名称
     */
    private String title;

    /**
     * 请求方式
     */
    private String method;

    /**
     * 请求URL
     */
    private String operUrl;

    /**
     * 主机地址
     */
    private String operIp;

    /**
     * 操作地点
     */
    private String operLocation;

    /**
     * 操作人员id
     */
    private String userId;

    /**
     * 用户名
     */
    private String nickname;

    /**
     * 请求参数
     */
    private String operParam;

    /**
     * 操作状态
     */
    private int status;

    /**
     * 错误消息
     */
    private String errorMsg;

    /**
     * 异常信息
     */
    private String errorData;

    /**
     * 接口耗时
     */
    private Long requestTime;

    /**
     * 创建时间
     */
    private DateTime createTime;

}
