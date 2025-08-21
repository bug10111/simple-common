package com.simple.common.mp.exception;

import com.simple.common.core.response.R;
import com.simple.common.core.utils.ExceptionHandlerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE) // 设置高优先级
public class MPExceptionHandler {

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<Object> handle(DuplicateKeyException exception) {
        ExceptionHandlerUtils.errorHandler(exception);
        return R.error(String.valueOf(HttpStatus.BAD_REQUEST.value()), "数据已存在");
    }

}
