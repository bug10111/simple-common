package com.simple.common.core.utils;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;

/**
 * Created with IntelliJ IDEA
 * Description: 时间操作工具类
 *
 * @author qty
 */
public class DateUtils extends DateUtil {

    private DateUtils() {
    }

    /**
     * 获取当前本地系统时间（显式指定yyyy-MM-dd HH:mm:ss格式，不依赖Hutool默认toString实现，
     * 避免库版本升级时默认输出格式静默变化；JVM默认时区语义不变）
     *
     * @return 当前系统时间字符串（yyyy-MM-dd HH:mm:ss 格式）
     */
    public static String getSystemDate() {
        return new DateTime().toString(DatePattern.NORM_DATETIME_PATTERN);
    }

    /**
     * 获取当前时间
     *
     * @return 当前时间字符串
     * @deprecated 该方法实现上就是返回本地系统时间（从未做过网络对时），继续调用只会得到语义错误的值；
     * 全仓库当前无调用方，改为快速失败让误用立刻暴露，请使用 {@link #getSystemDate()}
     */
    @Deprecated
    public static String getNetworkDate() {
        throw new UnsupportedOperationException("getNetworkDate已废弃（实现即本地时间），请使用getSystemDate()");
    }
}
