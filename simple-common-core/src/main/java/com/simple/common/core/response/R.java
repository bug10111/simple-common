package com.simple.common.core.response;

import com.simple.common.core.exception.AbstractException;
import com.simple.common.core.exception.DefaultExceptionEnum;
import lombok.Data;

import java.util.Objects;

/**
 * Created by IntelliJ IDEA
 * 统一数据返回,选择性使用，使用时，建议满足HttpState规范
 *
 * @author qty
 */
@Data
public class R<T> {

    //异常code
    private String code;

    //异常信息
    private String message;

    //数据载体
    private T data;

    public R() {
    }

    public R(AbstractException abstractException) {
        Objects.requireNonNull(abstractException, "abstractException must not be null");
        this.code = abstractException.getCode();
        this.message = abstractException.getMessage();
        this.data = null;
    }

    public R(AbstractException abstractException, T data) {
        this.code = abstractException.getCode();
        this.message = abstractException.getMessage();
        this.data = data;
    }

    public R(String code, String message) {
        this(code, message, null);
    }

    public R(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> ok() {
        return new R<>(DefaultExceptionEnum.OK);
    }

    public static <T> R<T> ok(T t) {
        return new R<>(DefaultExceptionEnum.OK, t);
    }

    public static <T> R<T> error() {
        return new R<>(DefaultExceptionEnum.ERROR);
    }

    /**
     * 携带错误数据的失败响应。注意：纯文本失败请使用 {@link #error(String)}，
     * 避免误把提示文本塞进 data 丢掉 message
     */
    public static <T> R<T> errorWithData(T data) {
        return new R<>(DefaultExceptionEnum.ERROR, data);
    }

    /**
     * 纯文本消息的失败响应（code=500）
     */
    public static <T> R<T> error(String message) {
        return new R<>(DefaultExceptionEnum.ERROR.getCode(), message);
    }

    public static <T> R<T> error(String code, String message) {
        return new R<>(code, message);
    }

    public static <T> R<T> error(String code, String message, T data) {
        return new R<>(code, message, data);
    }
}
