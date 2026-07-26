package com.simple.common.core.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;

/**
 * Created with IntelliJ IDEA
 * Description: 唯一ID生成
 *
 * @author qty
 */
public class IdUtils {

    /**
     * 基于本机IP完整哈希自动计算workerId和datacenterId的雪花算法实例。
     *
     * 将完整IP字符串哈希到0-1023（32×32个槽位），拆为高5位datacenterId + 低5位workerId，
     * 避免同网段IP第三段相同时datacenterId碰撞导致只能容纳32台的问题。
     */
    private static final Snowflake SNOWFLAKE;

    static {
        String ip = IPUtils.getIntranetIp();
        // 完整IP哈希映射到0-1023槽位，同网段也能均匀分散
        int hash = Math.abs(ip.hashCode()) % 1024;
        // 高5位：datacenterId（0-31），低5位：workerId（0-31）
        long datacenterId = hash >> 5;
        long workerId = hash & 0x1F;
        SNOWFLAKE = IdUtil.getSnowflake(workerId, datacenterId);
    }

    /**
     * 获取ID，雪花算法（基于本机IP保证分布式唯一）
     */
    public static String getSnowflakeNextIdStr() {
        return SNOWFLAKE.nextIdStr();
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
