package com.simple.common.mp.exception;

import com.simple.common.core.response.R;
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
 * <p>
 * MyBatis-Plus 唯一键冲突统一处理。
 * <p>
 * 优先级说明：Order 取「最低优先级上移 100」——仍先于 core 的全局兜底处理器
 * （Exception 兜底，最低优先级）命中 {@link DuplicateKeyException}；同时不再使用
 * HIGHEST_PRECEDENCE 霸占最前，业务工程可在更高优先级声明自己的唯一键冲突处理器完成覆盖。
 * <p>
 * 状态码说明：唯一键冲突是资源冲突（请求本身合法、仅唯一性约束不满足），使用 409 CONFLICT，
 * 与 400 参数错误（格式/校验失败）区分，便于调用方识别「记录已存在」语义。
 *
 * @author qty
 */
@Slf4j
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class MPExceptionHandler {

    /**
     * 唯一键冲突统一映射的 HTTP 状态码（响应头与响应体 code 使用同一来源，避免漂移）
     */
    private static final HttpStatus DUPLICATE_KEY_STATUS = HttpStatus.CONFLICT;

    /**
     * 唯一键冲突的对外提示文案
     */
    private static final String DUPLICATE_KEY_MESSAGE = "数据已存在";

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    public R<Object> handle(DuplicateKeyException exception) {
        //唯一键冲突是预期内的客户端侧冲突：仅记录 WARN 级别摘要，完整堆栈保留在 DEBUG，
        //避免污染 ERROR 告警，也避免把完整 SQL/唯一键值全量打进错误日志
        log.warn("数据唯一键冲突: {}", exception.getMessage());
        log.debug("数据唯一键冲突堆栈", exception);
        return R.error(String.valueOf(DUPLICATE_KEY_STATUS.value()), DUPLICATE_KEY_MESSAGE);
    }

}
