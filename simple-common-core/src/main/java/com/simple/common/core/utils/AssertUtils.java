package com.simple.common.core.utils;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.simple.common.core.exception.AbstractException;
import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.exception.DefaultExceptionEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Slf4j
public class AssertUtils {

    /**
     * 判断指定对象是否为空，支持：
     * 1. CharSequence
     * 2. Map
     * 3. Iterable
     * 4. Iterator
     * 5. Array
     *
     * @param object   对象
     * @param errorMsg 需要抛出的异常数据
     */
    public static void notEmpty(Object object, String errorMsg) {
        isTrue(ObjUtil.isNotEmpty(object), errorMsg);
    }

    /**
     * 判断指定对象是否为空，支持：
     * 1. CharSequence
     * 2. Map
     * 3. Iterable
     * 4. Iterator
     * 5. Array
     *
     * @param object   对象
     * @param errorMsg 需要抛出的异常数据
     * @param params   格式化参数
     */
    public static void notEmpty(Object object, String errorMsg, String errorLog, Object... params) {
        isTrue(ObjUtil.isNotEmpty(object), errorMsg, StrUtil.format(errorLog, params));
    }

    /**
     * 判断指定对象是否为空，支持：
     * 1. CharSequence
     * 2. Map
     * 3. Iterable
     * 4. Iterator
     * 5. Array
     *
     * @param object   对象
     * @param errorMsg 需要抛出的异常数据
     * @param params   格式化参数
     */
    public static void notEmptyParams(Object object, String errorMsg, Object... params) {
        notEmpty(object, StrUtil.format(errorMsg, params));
    }

    /**
     * 判断指定对象是否为空，支持：
     * 1. CharSequence
     * 2. Map
     * 3. Iterable
     * 4. Iterator
     * 5. Array
     *
     * @param object    对象
     * @param exception 需要抛出的异常
     */
    public static void notEmpty(Object object, AbstractException exception) {
        isTrue(ObjUtil.isNotEmpty(object), exception);
    }

    /**
     * 判断指定对象是否为空，支持：
     * 1. CharSequence
     * 2. Map
     * 3. Iterable
     * 4. Iterator
     * 5. Array
     *
     * @param object    对象
     * @param exception 需要抛出的异常
     * @param errorMsg  重写异常信息
     */
    public static void notEmpty(Object object, AbstractException exception, String errorMsg) {
        isTrue(ObjUtil.isNotEmpty(object), exception, errorMsg);
    }

    /**
     * 判断指定对象是否为空，支持：
     * 1. CharSequence
     * 2. Map
     * 3. Iterable
     * 4. Iterator
     * 5. Array
     *
     * @param object    对象
     * @param exception 需要抛出的异常
     */
    public static void notEmpty(Object object, AbstractException exception, String errorLog, Object... params) {
        isTrue(ObjUtil.isNotEmpty(object), exception, errorLog, params);
    }

    /**
     * 断言是否为真，如果为 false 抛出 DefaultException 异常
     *
     * @param expression 布尔值
     * @param errorStr   需要抛出的异常信息
     */
    public static void isTrue(boolean expression, String errorStr) {
        if (!expression) AssertUtils.error(errorStr);
    }

    /**
     * 断言是否为真，如果为 false 抛出 DefaultException 异常
     *
     * @param expression 布尔值
     * @param errorStr   需要抛出的异常信息
     * @param errorLog   需要打印的异常日志
     */
    public static void isTrue(boolean expression, String errorStr, String errorLog) {
        if (!expression) AssertUtils.error(errorStr, errorLog);
    }

    /**
     * 断言是否为真，如果为 false 抛出 DefaultException 异常
     *
     * @param expression 布尔值
     * @param errorStr   需要抛出的异常信息
     * @param params     格式化参数
     */
    public static void isTrueParams(boolean expression, String errorStr, Object... params) {
        if (!expression) AssertUtils.error(StrUtil.format(errorStr, params));
    }

    /**
     * 断言是否为真，如果为 false 抛出 DefaultException 异常
     *
     * @param expression 布尔值
     * @param errorStr   需要抛出的异常信息
     * @param errorLog   需要打印的异常日志
     * @param params     格式化参数
     */
    public static void isTrue(boolean expression, String errorStr, String errorLog, Object... params) {
        if (!expression) AssertUtils.error(errorStr, StrUtil.format(errorLog, params));
    }

    /**
     * 断言是否为真，如果为 false 抛出 DefaultException 异常
     *
     * @param expression        布尔值
     * @param abstractException 异常枚举
     * @param errorLog          需要打印的异常日志
     */
    public static void isTrue(boolean expression, AbstractException abstractException, String errorLog) {
        if (!expression) AssertUtils.error(abstractException, errorLog);
    }

    /**
     * 断言是否为真，如果为 false 抛出 DefaultException 异常
     *
     * @param expression        布尔值
     * @param abstractException 异常枚举
     */
    public static void isTrue(boolean expression, AbstractException abstractException) {
        if (!expression) AssertUtils.error(abstractException);
    }

    /**
     * 断言是否为真，如果为 false 抛出 DefaultException 异常
     *
     * @param expression        布尔值
     * @param abstractException 异常枚举
     * @param errorLog          需要打印的异常日志
     * @param params            格式化参数
     */
    public static void isTrue(boolean expression, AbstractException abstractException, String errorLog, Object... params) {
        if (!expression) AssertUtils.error(abstractException, StrUtil.format(errorLog, params));
    }

    /**
     * 抛出指定异常
     *
     * @param abstractException 异常枚举
     */
    public static void error(AbstractException abstractException) {
        AssertUtils.basError(abstractException.getCode(), abstractException.getMessage());
    }

    /**
     * 抛出指定异常
     *
     * @param abstractException 异常枚举
     * @param errorLog          需要打印的异常日志
     */
    public static void error(AbstractException abstractException, String errorLog) {
        AssertUtils.basError(abstractException.getCode(), abstractException.getMessage(), errorLog);
    }

    /**
     * 抛出指定异常
     *
     * @param abstractException 异常枚举
     * @param errorLog          需要打印的异常日志
     * @param params            格式化参数
     */
    public static void error(AbstractException abstractException, String errorLog, Object... params) {
        AssertUtils.basError(abstractException.getCode(), abstractException.getMessage(), StrUtil.format(errorLog, params));
    }

    /**
     * 抛出指定异常
     *
     * @param error 指定文本
     */
    public static void error(String error) {
        AssertUtils.basError(DefaultExceptionEnum.ERROR.getCode(), error);
    }

    /**
     * 抛出指定异常
     *
     * @param error  指定文本
     * @param params 格式化参数
     */
    public static void errorParams(String error, Object... params) {
        AssertUtils.basError(DefaultExceptionEnum.ERROR.getCode(), StrUtil.format(error, params));
    }

    /**
     * 抛出指定异常
     *
     * @param error    指定文本
     * @param errorLog 需要打印的异常日志
     */
    public static void error(String error, String errorLog) {
        AssertUtils.basError(DefaultExceptionEnum.ERROR.getCode(), error, errorLog);
    }

    /**
     * 抛出指定异常
     *
     * @param error 指定文本
     * @param Data  异常时候需要返回的对象
     */
    public static void error(String error, Object Data) {
        AssertUtils.basError(DefaultExceptionEnum.ERROR.getCode(), error, Data);
    }

    /**
     * 抛出指定异常
     *
     * @param error    指定文本
     * @param errorLog 需要打印的异常日志
     * @param params   格式化参数
     */
    public static void error(String error, String errorLog, Object... params) {
        AssertUtils.basError(DefaultExceptionEnum.ERROR.getCode(), error, StrUtil.format(errorLog, params));
    }

    /**
     * 抛出指定异常
     *
     * @param code  指定code
     * @param error 指定文本
     */
    private static void basError(String code, String error) {
        throw new DefaultException(code, error, null);
    }

    /**
     * 抛出指定异常
     *
     * @param code     指定code
     * @param error    指定文本
     * @param errorLog 需要打印的异常日志
     */
    private static void basError(String code, String error, String errorLog) {
        log.error(errorLog);
        throw new DefaultException(code, error, null);
    }

    /**
     * 抛出指定异常
     *
     * @param code     指定code
     * @param error    指定文本
     * @param errorLog 需要打印的异常日志
     */
    private static void basError(String code, String error, Object data, String errorLog) {
        log.error(errorLog);
        throw new DefaultException(code, error, data);
    }

    /**
     * 抛出指定异常
     *
     * @param code  指定code
     * @param error 指定文本
     */
    private static void basError(String code, String error, Object data) {
        throw new DefaultException(code, error, data);
    }
}
