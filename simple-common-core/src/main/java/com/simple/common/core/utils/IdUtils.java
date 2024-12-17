package com.simple.common.core.utils;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;

/**
 * Created with IntelliJ IDEA
 * Description: 唯一ID生成
 *
 * @author 兄台丶请冷静
 */
public class IdUtils {

    /**
     * 获取ID，雪花算法
     */
    public static String getSnowflakeNextIdStr() {
        return IdUtil.getSnowflakeNextIdStr();
    }

    /**
     * 获取ID，uuid去掉横线
     */
    public static String getFastSimpleUUID() {
        return IdUtil.fastSimpleUUID();
    }

    /**
     * 获取ID，uuid
     */
    public static String getFastUUID() {
        return IdUtil.simpleUUID();
    }

    /**
     * 生成随机字符串-纯数字
     */
    public static String randomNumbers() {
        return RandomUtil.randomNumbers(9);
    }

    /**
     * 生成随机字符串-纯数字
     *
     * @param length 长度
     */
    public static String randomNumbers(int length) {
        return RandomUtil.randomNumbers(length);
    }

}
