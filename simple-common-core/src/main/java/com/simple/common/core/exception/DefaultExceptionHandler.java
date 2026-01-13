package com.simple.common.core.exception;

import com.simple.common.core.common.constant.CoreConstant;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.ExceptionHandlerUtils;
import com.simple.common.core.utils.HttpServletUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@ControllerAdvice
public class DefaultExceptionHandler {

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ResponseBody
    public R<Object> handle(HttpRequestMethodNotSupportedException exception) {
        ExceptionHandlerUtils.errorHandler(exception);
        String str = "请求的方法，不允许使用" + exception.getMethod() + "方法访问。支持的方法：" + exception.getSupportedHttpMethods();
        return R.error(String.valueOf(HttpStatus.METHOD_NOT_ALLOWED.value()), str);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<Object> handle(IllegalArgumentException exception) {
        ExceptionHandlerUtils.errorHandler(exception);
        return R.error(String.valueOf(HttpStatus.BAD_REQUEST.value()), exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<Object> handle(IllegalStateException exception) {
        ExceptionHandlerUtils.errorHandler(exception);
        return R.error(String.valueOf(HttpStatus.BAD_REQUEST.value()), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public R<Object> handle(MethodArgumentNotValidException exception) {
        ExceptionHandlerUtils.errorHandler(exception);
        //        List<ObjectError> allErrors = exception.getBindingResult().getAllErrors();
        //        List<String> list = allErrors.stream().map(DefaultMessageSourceResolvable::getDefaultMessage).toList();
        return R.error(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), exception.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public R<Object> handle(Exception exception) {
        ExceptionHandlerUtils.errorHandler(exception);
        R<Object> result = new R<>();
        if (exception instanceof DefaultException defaultException) {
            result.setCode(defaultException.getCode());
        } else {
            result.setCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
        result.setMessage(exception.getMessage());
        result.setData(null);
        return result;
    }
}
