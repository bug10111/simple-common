package com.simple.common.core.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Created by IntelliJ IDEA
 * 自定义默认异常
 *
 * @author 兄台丶请冷静
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class DefaultException extends RuntimeException {

    //异常code
    private String code;

    //异常信息
    private String message;

    //数据载体
    private Object data;

    public DefaultException(String code, String message) {
        this.code = code;
        this.message = message;
        this.data = null;
    }

    public DefaultException(String code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public DefaultException(AbstractException abstractException) {
        this.code = abstractException.getCode();
        this.message = abstractException.getMessage();
        this.data = null;
    }

    public DefaultException(AbstractException abstractException, String message) {
        this.code = abstractException.getCode();
        this.message = message;
        this.data = null;
    }

    public DefaultException(String message) {
        this.code = DefaultExceptionEnum.ERROR.getCode();
        this.message = message;
        this.data = null;
    }
}
