package com.simple.common.core.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;

/**
 * Created with IntelliJ IDEA
 * Description: 唯一ID生成
 * <p>workerId分配契约：优先读系统属性 {@code simple.id.worker-id}/{@code simple.id.datacenter-id}
 * （多实例部署在同机/同IP时必须显式分配，IP哈希槽位只有1024个且同JVM内去重无法跨进程保证）；
 * 未配置时回退"本机内网IP哈希到0-1023槽位，高5位datacenterId + 低5位workerId"；
 * IP获取失败时再回退随机槽位并告警（保证类可用，代价是碰撞概率升高，由显式配置兜底消除）。</p>
 *
 * @author qty
 */
public class IdUtils {

    /**
     * workerId系统属性名
     */
    public static final String WORKER_ID_PROPERTY = "simple.id.worker-id";

    /**
     * datacenterId系统属性名
     */
    public static final String DATACENTER_ID_PROPERTY = "simple.id.datacenter-id";

    private static final Snowflake SNOWFLAKE;

    static {
        long datacenterId;
        long workerId;
        Long configuredWorker = configuredId(WORKER_ID_PROPERTY);
        Long configuredDatacenter = configuredId(DATACENTER_ID_PROPERTY);
        if (configuredWorker != null && configuredDatacenter != null) {
            // 显式配置优先：同机多实例部署必须走这里保证跨进程唯一
            workerId = configuredWorker;
            datacenterId = configuredDatacenter;
        } else {
            String ip;
            try {
                ip = IPUtils.getIntranetIp();
            } catch (Exception e) {
                // 静态初始化失败会把整个工具类毒成NoClassDefFoundError（连uuid接口都不可用），
                // 这里降级为随机槽位并告警，由运维用显式配置消除碰撞概率
                ip = null;
            }
            if (ip == null) {
                workerId = RandomUtil.randomLong(0, 32);
                datacenterId = RandomUtil.randomLong(0, 32);
            } else {
                // 完整IP哈希映射到0-1023槽位，同网段也能均匀分散；floorMod杜绝Integer.MIN_VALUE取绝对值为负
                int hash = Math.floorMod(ip.hashCode(), 1024);
                // 高5位：datacenterId（0-31），低5位：workerId（0-31）
                datacenterId = hash >> 5;
                workerId = hash & 0x1F;
            }
        }
        SNOWFLAKE = IdUtil.getSnowflake(workerId, datacenterId);
    }

    private IdUtils() {
    }

    private static Long configuredId(String propertyName) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            return null;
        }
        long id = Long.parseLong(value.trim());
        if (id < 0 || id > 31) {
            throw new IllegalArgumentException(propertyName + " 必须在0~31之间: " + value);
        }
        return id;
    }

    /**
     * 获取ID，雪花算法（workerId分配契约见类说明）
     *
     * @return 雪花算法生成的唯一ID字符串
     */
    public static String getSnowflakeNextIdStr() {
        return SNOWFLAKE.nextIdStr();
    }

    /**
     * 获取ID，uuid去掉横线
     *
     * @return 不含横线的32位uuid字符串
     */
    public static String getFastSimpleUUID() {
        return IdUtil.fastSimpleUUID();
    }

    /**
     * 获取ID，uuid（带横线）
     *
     * @return 含横线的36位uuid字符串
     */
    public static String getFastUUID() {
        return IdUtil.fastUUID();
    }

    /**
     * 生成随机字符串-纯数字
     *
     * @return 9位纯数字随机字符串
     */
    public static String randomNumbers() {
        return RandomUtil.randomNumbers(9);
    }

    /**
     * 生成随机字符串-纯数字
     *
     * @param length 长度
     * @return 指定长度的纯数字随机字符串
     */
    public static String randomNumbers(int length) {
        return RandomUtil.randomNumbers(length);
    }

}
