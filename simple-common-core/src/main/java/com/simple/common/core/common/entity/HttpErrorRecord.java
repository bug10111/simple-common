package com.simple.common.core.common.entity;

import cn.hutool.http.HttpResponse;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.exception.DefaultExceptionEnum;
import com.simple.common.core.utils.JsonUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * Created with IntelliJ IDEA
 * Description: http请求工具的异常信息记录
 * <p>安全与资源说明：{@link #setExecute(HttpResponse)} 会即刻提取状态码与响应体，
 * 便于调用方随后安全关闭底层连接；响应体不再随 toString 序列化，异常消息中仅保留截断后的响应体，
 * 防止网关错误页/内部诊断信息全量外泄。</p>
 *
 * @author qty
 */
@Slf4j
@Data
@Accessors(chain = true)
@Schema(description = "http请求工具的异常信息记录")
public class HttpErrorRecord {

    /**
     * 异常消息中携带响应体的最大长度
     */
    private static final int MAX_BODY_IN_MESSAGE = 500;

    @Schema(description = "请求结果")
    @ToString.Exclude
    @JsonIgnore
    private HttpResponse execute;

    @Schema(description = "HTTP状态码")
    private int status;

    @Schema(description = "响应体")
    private String body;

    /**
     * 记录响应：立即提取状态码与响应体（此后底层响应可被调用方安全关闭）
     */
    public HttpErrorRecord setExecute(HttpResponse execute) {
        this.execute = execute;
        if (execute != null) {
            this.status = execute.getStatus();
            this.body = execute.body();
        }
        return this;
    }

    /**
     * 获取结果字符串（响应体在setExecute时已提取）
     */
    public String getStr() {
        return body;
    }

    /**
     * 获取结果对象
     *
     * @param clazz 结果对象class
     * @param <T>   结果对象
     * @return 解析失败（空体/非JSON错误页）时返回null，不再抛出解析异常掩盖原始HTTP错误
     */
    public <T> T getObj(Class<T> clazz) {
        if (StrUtil.isBlank(body)) {
            return null;
        }
        try {
            return JsonUtils.toJsonObj(body, clazz);
        } catch (Exception e) {
            log.warn("HTTP错误响应体解析失败，status={}, body截断=[{}]", status, StrUtil.subPre(body, 200), e);
            return null;
        }
    }

    /**
     * 获取异常对象（响应体截断后放入消息，避免token等敏感信息全量进入日志/响应）
     */
    public DefaultException getException() {
        return new DefaultException(DefaultExceptionEnum.ERROR, "HTTP " + status + ": " + StrUtil.subPre(body, MAX_BODY_IN_MESSAGE));
    }

    /**
     * 获取异常对象
     */
    public DefaultException getException(String errorStr) {
        return new DefaultException(DefaultExceptionEnum.ERROR, errorStr);
    }
}
