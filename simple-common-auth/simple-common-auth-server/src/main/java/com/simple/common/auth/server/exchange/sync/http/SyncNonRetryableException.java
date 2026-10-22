package com.simple.common.auth.server.exchange.sync.http;

/**
 * 实例回调不可重试异常（投递引擎回调链路专用）。
 * <p>
 * 语义：目标实例对本次同步载荷给出了确定性拒绝（如 401 凭证失效、413 载荷超限、
 * 404 回调路径不存在等客户端错误），重试同样的投递注定失败——投递 worker 捕获本异常
 * 后直接落失败记录收口，不再消耗重试预算。
 * </p>
 *
 * @author qty
 */
public class SyncNonRetryableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造不可重试回调异常
     *
     * @param message 错误信息（含状态码与脱敏后的回调地址）
     */
    public SyncNonRetryableException(String message) {
        super(message);
    }

    /**
     * 构造不可重试回调异常
     *
     * @param message 错误信息
     * @param cause   原始异常
     */
    public SyncNonRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
