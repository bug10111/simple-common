package com.simple.common.rabbitmq.common.entity;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 * Description: 消息队列消息载体
 * <p>
 * {@link #getId()} 与 {@link #getSendTime()} 均带惰性兜底：即使调用方忘记调用 {@link #initialization()}，
 * 读取时也会补齐，保证消息始终携带唯一 id（消费端幂等/去重依赖该 id），避免静默产生 id 为 null 的消息。
 * </p>
 *
 * @author qty
 */
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DefaultMessage {

    //发送的事件对象json
    private String msgData;

    //发送的时间
    private String sendTime;

    //消息唯一id
    private String id;

    /**
     * 获取消息体 JSON 载荷；消费侧反序列化处理器参数与报文合法性校验依赖本读侧。
     * 与 id/sendTime 的惰性 getter 不同，载荷无生成语义，原样返回。
     */
    public String getMsgData() {
        return msgData;
    }

    /**
     * 初始化消息元数据：id 为空（null 或空白）时生成雪花 id，发送时间为空时按统一格式补齐。
     * <p>
     * 注意：重复调用不会覆盖已存在的值。
     * </p>
     */
    public void initialization() {
        if (StrUtil.isBlank(id)) {
            this.id = IdUtil.getSnowflakeNextIdStr();
        }
        if (StrUtil.isBlank(sendTime)) {
            this.sendTime = DateUtil.date().toString(DatePattern.NORM_DATETIME_MS_FORMAT);
        }
    }

    /**
     * 获取消息唯一 id；为空时惰性生成雪花 id，保证任何发布路径拿到的 id 都非空。
     */
    public String getId() {
        if (StrUtil.isBlank(id)) {
            this.id = IdUtil.getSnowflakeNextIdStr();
        }
        return id;
    }

    /**
     * 获取发送时间；为空时按 {@code yyyy-MM-dd HH:mm:ss.SSS} 格式补齐当前时间。
     * 使用显式固定格式并保留毫秒，避免依赖环境默认格式导致生产/消费端解析不一致。
     */
    public String getSendTime() {
        if (StrUtil.isBlank(sendTime)) {
            this.sendTime = DateUtil.date().toString(DatePattern.NORM_DATETIME_MS_FORMAT);
        }
        return sendTime;
    }
}
