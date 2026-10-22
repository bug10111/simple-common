package com.simple.common.core.exception;

import com.simple.common.core.response.R;
import com.simple.common.core.utils.ExceptionHandlerUtils;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    /**
     * 是否在响应中附带异常堆栈详情：仅显式配置开启时生效，防止生产环境把内部细节泄露给客户端
     */
    @Value("${simple.exception.include-stacktrace:false}")
    private boolean includeStackTrace;

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ResponseBody
    public R<Object> handle(HttpRequestMethodNotSupportedException exception) {
        ExceptionHandlerUtils.errorHandler(exception);
        // 拼装方法不支持提示,附上当前方法与受支持的方法列表
        String message = "请求的方法，不允许使用" + exception.getMethod() + "方法访问。支持的方法：" + exception.getSupportedHttpMethods();
        return errorResponse(HttpStatus.METHOD_NOT_ALLOWED, message);
    }

    // 非法参数属请求侧错误,映射400；IllegalStateException单独归服务端500,避免把内部状态问题误报为客户端错误
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<Object> handle(IllegalArgumentException exception) {
        ExceptionHandlerUtils.warnHandler(exception);
        return errorResponse(HttpStatus.BAD_REQUEST, orDefault(exception));
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public R<Object> handle(IllegalStateException exception) {
        ExceptionHandlerUtils.errorHandler(exception);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, orDefault(exception));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<Object> handle(MethodArgumentNotValidException exception) {
        ExceptionHandlerUtils.warnHandler(exception);
        // 取第一条校验错误的默认消息作为响应消息,无字段级错误时兜底,避免索引越界
        var errors = exception.getBindingResult().getAllErrors();
        String message = errors.isEmpty() ? "参数校验失败" : errors.get(0).getDefaultMessage();
        return errorResponse(HttpStatus.BAD_REQUEST, message);
    }

    // 表单绑定、参数缺失、请求体不可读、参数类型转换失败均属客户端请求侧错误,映射400避免落入500兜底
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<Object> handle(BindException exception) {
        ExceptionHandlerUtils.warnHandler(exception);
        return errorResponse(HttpStatus.BAD_REQUEST, orDefault(exception));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<Object> handle(MissingServletRequestParameterException exception) {
        ExceptionHandlerUtils.warnHandler(exception);
        return errorResponse(HttpStatus.BAD_REQUEST, "缺少必要参数：" + exception.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<Object> handle(HttpMessageNotReadableException exception) {
        ExceptionHandlerUtils.warnHandler(exception);
        return errorResponse(HttpStatus.BAD_REQUEST, "请求体格式错误或不可读");
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<Object> handleParam(Exception exception) {
        ExceptionHandlerUtils.warnHandler(exception);
        return errorResponse(HttpStatus.BAD_REQUEST, "请求参数不合法：" + exception.getMessage());
    }

    // 静态资源与未匹配路由同属请求侧资源不存在,映射404避免落入500兜底
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public R<Object> handle(NoResourceFoundException exception) {
        ExceptionHandlerUtils.warnHandler(exception);
        // 拼装资源不存在提示,附上请求的资源路径
        String message = "请求的资源不存在：" + exception.getResourcePath();
        return errorResponse(HttpStatus.NOT_FOUND, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<R<Object>> handle(Exception exception) {
        ExceptionHandlerUtils.errorHandler(exception);
        return buildExceptionResponse(exception);
    }

    // 按异常类型组装统一响应:框架业务异常透出业务码/业务消息/业务数据,未知异常透出兜底消息并按配置附加异常详情
    private ResponseEntity<R<Object>> buildExceptionResponse(Exception exception) {
        // 框架业务异常携带业务码与业务消息,直接透出;业务码为合法4xx/5xx时映射为对应HTTP状态,其余统一500
        if (exception instanceof DefaultException defaultException) {
            // 业务数据一并返回,不再丢弃
            R<Object> body = R.error(defaultException.getCode(), defaultException.getMessage(), defaultException.getData());
            return ResponseEntity.status(resolveStatus(defaultException.getCode())).body(body);
        }
        // 兜底响应,详情按配置决定是否携带(默认关闭,防内部信息泄露)
        String detail = exceptionDetail(exception);
        R<Object> body = R.error(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), "系统繁忙，请稍后再试", detail);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // 业务码为合法HTTP 4xx/5xx时映射为对应状态码,其余(含非数字业务码)回落500
    private HttpStatus resolveStatus(String code) {
        try {
            int status = Integer.parseInt(code);
            if (status >= 400 && status <= 599) {
                return HttpStatus.valueOf(status);
            }
        } catch (IllegalArgumentException ignored) {
            // 非HTTP语义业务码,统一按500处理
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    // 异常消息为null时(仅cause构造)兜底,避免响应message出现null
    private String orDefault(Exception exception) {
        return exception.getMessage() == null ? "请求处理失败" : exception.getMessage();
    }

    // 仅显式配置simple.exception.include-stacktrace=true时透出完整异常堆栈(含cause链),其余环境返回null
    private String exceptionDetail(Exception exception) {
        if (!includeStackTrace) {
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
