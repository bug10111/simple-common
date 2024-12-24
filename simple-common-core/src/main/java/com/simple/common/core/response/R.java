package com.simple.common.core.response;

import com.simple.common.core.exception.AbstractException;
import com.simple.common.core.exception.DefaultExceptionEnum;
import lombok.Data;

/**
 * Created by IntelliJ IDEA on 2023/10/28 14:51
 * 统一数据返回,选择性使用，使用时，建议满足HttpState规范
 *
 * @author 兄台丶请冷静
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
        this.code = code;
        this.message = message;
        this.data = null;
    }

    public static R<Object> ok() {
        return new R<>(DefaultExceptionEnum.OK);
    }

    public static <T> R<T> ok(T t) {
        return new R<>(DefaultExceptionEnum.OK, t);
    }

    public static <T> R<T> error() {
        return new R<>(DefaultExceptionEnum.ERROR);
    }

    public static <T> R<T> error(T t) {
        return new R<>(DefaultExceptionEnum.ERROR, t);
    }

    public static <T> R<T> error(String code, String message) {
        return new R<>(code, message);
    }
}
