package com.simple.common.core.utils;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;

/**
 * Created with IntelliJ IDEA
 * Description: 时间操作工具类
 *
 * @author qty
 */
public class DateUtils extends DateUtil {

    /**
     * 获取当前本地系统时间
     *
     * @return 当前系统时间字符串（yyyy-MM-dd HH:mm:ss 格式）
     */
    public static String getSystemDate() {
        return new DateTime().toString();
    }

    /**
     * 获取当前时间
     *
     * @return 当前时间字符串
     * @deprecated 该方法实际返回本地系统时间而非网络时间，请使用 {@link #getSystemDate()}
     */
    @Deprecated
    public static String getNetworkDate() {
        return getSystemDate();
    }
}
