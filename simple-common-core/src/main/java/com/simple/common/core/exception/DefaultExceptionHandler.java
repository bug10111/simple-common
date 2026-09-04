package com.simple.common.core.exception;

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

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 全局异常统一处理器,按异常类型映射状态码与业务消息
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
        // 拼装方法不支持提示,附上当前方法与受支持的方法列表
        String message = "请求的方法，不允许使用" + exception.getMethod() + "方法访问。支持的方法：" + exception.getSupportedHttpMethods();
        return errorResponse(HttpStatus.METHOD_NOT_ALLOWED, message);
    }

    // 非法参数与非法状态同属请求侧错误,状态码与消息口径一致,合并处理
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<Object> handle(RuntimeException exception) {
        ExceptionHandlerUtils.errorHandler(exception);
        return errorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public R<Object> handle(MethodArgumentNotValidException exception) {
        ExceptionHandlerUtils.errorHandler(exception);
        // 取第一条校验错误的默认消息作为响应消息
        String message = exception.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public R<Object> handle(Exception exception) {
        ExceptionHandlerUtils.errorHandler(exception);
        return buildExceptionResponse(exception);
    }

    // 按异常类型组装统一响应:框架业务异常透出业务消息,未知异常透出兜底消息并按环境附加异常详情
    private R<Object> buildExceptionResponse(Exception exception) {
        // 框架业务异常携带业务码与业务消息,直接透出
        if (exception instanceof DefaultException defaultException) {
            return R.error(defaultException.getCode(), defaultException.getMessage());
        }
        // 未知异常记录完整日志后仅透出兜底消息,详情按环境决定是否携带
        log.error("系统内部异常，请求: {}", HttpServletUtils.getRequest().getRequestURI(), exception);
        String detail = exceptionDetail(exception);
        return R.error(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), "系统繁忙，请稍后再试", detail);
    }

    // 仅local/dev环境透出完整异常堆栈(含cause链),其余环境返回null
    private String exceptionDetail(Exception exception) {
        if (!log.isDebugEnabled()) {
            return null;
        }

        // 将完整堆栈写入字符串作为响应data,便于联调期在响应体内直接定位根因
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    // 统一错误响应组装,收敛各handler的重复构建
    private R<Object> errorResponse(HttpStatus status, String message) {
        return R.error(String.valueOf(status.value()), message);
    }
}
