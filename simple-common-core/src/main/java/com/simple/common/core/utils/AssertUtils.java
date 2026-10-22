package com.simple.common.core.utils;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.simple.common.core.exception.AbstractException;
import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.exception.DefaultExceptionEnum;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
public final class AssertUtils {

    private AssertUtils() {
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
     */
    public static void notEmpty(Object object, String errorMsg) {
        isTrue(ObjUtil.isNotEmpty(object), errorMsg);
    }

    /**
     * 判断指定对象是否不为null
     *
     * @param object   对象
     * @param errorMsg 需要抛出的异常数据
     */
    public static void notNull(Object object, String errorMsg) {
        isTrue(object != null, errorMsg);
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
    public static void notEmpty(Object object, String errorMsg,  Object... params) {
        isTrue(ObjUtil.isNotEmpty(object), StrUtil.format(errorMsg, params));
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
     * @param errorMsg  重写异常信息
     */
    public static void notEmpty(Object object, AbstractException exception, String errorMsg,Object... params) {
        isTrue(ObjUtil.isNotEmpty(object), exception,  StrUtil.format(errorMsg, params));
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
     * @param params     格式化参数
     */
    public static void isTrue(boolean expression, String errorStr, Object... params) {
        if (!expression) AssertUtils.error(StrUtil.format(errorStr, params));
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
     * @param errorStr          异常信息
     */
    public static void isTrue(boolean expression, AbstractException abstractException, String errorStr) {
        if (!expression) AssertUtils.error(abstractException, errorStr);
    }

    /**
     * 断言是否为真，如果为 false 抛出 DefaultException 异常
     *
     * @param expression        布尔值
     * @param abstractException 异常枚举
     * @param errorStr          异常信息
     */
    public static void isTrue(boolean expression, AbstractException abstractException, String errorStr, Object... params) {
        if (!expression) AssertUtils.error(abstractException, StrUtil.format(errorStr, params));
    }


    /**
     * 抛出指定异常
     *
     * @param abstractException 异常枚举
     */
    public static void error(AbstractException abstractException) {
        AssertUtils.baseError(abstractException.getCode(), abstractException.getMessage());
    }

    /**
     * 抛出指定异常，errorStr 作为重写后的异常信息（覆盖枚举默认信息），不携带附加数据
     *
     * @param abstractException 异常枚举
     * @param errorStr          重写后的异常信息
     */
    public static void error(AbstractException abstractException, String errorStr) {
        AssertUtils.baseError(abstractException.getCode(), errorStr);
    }

    /**
     * 抛出指定异常，errorStr 作为重写后的异常信息（占位符用 params 格式化）
     *
     * @param abstractException 异常枚举
     * @param errorStr          重写后的异常信息
     * @param params            格式化参数
     */
    public static void error(AbstractException abstractException, String errorStr, Object... params) {
        AssertUtils.baseError(abstractException.getCode(), StrUtil.format(errorStr, params));
    }

    /**
     * 抛出指定异常
     *
     * @param error 指定文本
     */
    public static void error(String error) {
        AssertUtils.baseError(DefaultExceptionEnum.ERROR.getCode(), error);
    }

    /**
     * 抛出指定异常，error 中 {} 占位符由 params 格式化
     *
     * @param error  指定文本
     * @param params 格式化参数
     */
    public static void error(String error, Object... params) {
        baseError(DefaultExceptionEnum.ERROR.getCode(), StrUtil.format(error, params));
    }

    /**
     * 抛出携带附加数据的异常（语义明确的重载，避免与格式化重载混淆）
     *
     * @param error 指定文本
     * @param data  异常时需要返回的对象
     */
    public static void errorWithData(String error, Object data) {
        baseError(DefaultExceptionEnum.ERROR.getCode(), error, data);
    }

    /**
     * 抛出指定异常
     *
     * @param code  指定code
     * @param error 指定文本
     */
    private static void baseError(String code, String error) {
        if (DefaultExceptionEnum.OK.getCode().equals(code)) {
            throw new IllegalArgumentException("不能使用成功码[200]抛出异常, 请检查异常枚举定义");
        }
        throw new DefaultException(code, error, null);
    }

    /**
     * 抛出指定异常
     *
     * @param code  指定code
     * @param error 指定文本
     * @param data  异常时返回的对象
     */
    private static void baseError(String code, String error, Object data) {
        if (DefaultExceptionEnum.OK.getCode().equals(code)) {
            throw new IllegalArgumentException("不能使用成功码[200]抛出异常, 请检查异常枚举定义");
        }
        throw new DefaultException(code, error, data);
    }
}
