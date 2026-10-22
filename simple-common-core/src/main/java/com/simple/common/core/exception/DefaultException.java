package com.simple.common.core.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Created by IntelliJ IDEA
 * 自定义默认异常
 * <p>异常信息由 {@link Throwable#getMessage()} 单一来源承载（构造时传入 super），
 * 不再重复声明 message 字段，避免 Throwable 内部状态与 Lombok 生成方法二义。</p>
 *
 * @author qty
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class DefaultException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    //异常code
    private String code;

    //数据载体
    private Object data;

    public DefaultException(String code, String message) {
        super(message);
        this.code = code;
        this.data = null;
    }

    public DefaultException(String code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public DefaultException(AbstractException abstractException) {
        super(abstractException.getMessage());
        this.code = abstractException.getCode();
        this.data = null;
    }

    /**
     * 包装底层异常，保留原始堆栈与cause链
     */
    public DefaultException(AbstractException abstractException, Throwable cause) {
        super(abstractException.getMessage(), cause);
        this.code = abstractException.getCode();
        this.data = null;
    }

    public DefaultException(AbstractException abstractException, String message) {
        super(message);
        this.code = abstractException.getCode();
        this.data = null;
    }

    public DefaultException(String message) {
        super(message);
        this.code = DefaultExceptionEnum.ERROR.getCode();
        this.data = null;
    }
}
